package ch.gradedesk.ui.view;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.model.CourseModule;
import ch.gradedesk.model.Semester;
import ch.gradedesk.service.GradeBookService;
import ch.gradedesk.service.GradeValidator;
import ch.gradedesk.service.ValidationException;
import ch.gradedesk.ui.AppServices;
import javafx.geometry.Insets;
import ch.gradedesk.ui.ViewController;
import ch.gradedesk.ui.control.Dialogs;
import ch.gradedesk.ui.control.GradeTrack;
import ch.gradedesk.ui.util.Formats;
import ch.gradedesk.ui.util.Icons;
import ch.gradedesk.ui.util.Theme;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.List;

/**
 * Die Übersicht: pro Semester eine Karte, darin jedes Modul mit Notenband,
 * gerundeter und ungerundeter Note sowie dem Status in Worten.
 *
 * <p>Diese Ansicht ist der Startbildschirm, weil «wie stehe ich gerade?» die
 * häufigste Frage ist. Sie zeigt den Stand, ohne dass etwas aufgeklappt werden
 * muss.</p>
 */
public final class OverviewController implements ViewController {

    @FXML
    private Label subtitleLabel;
    @FXML
    private VBox bodyBox;
    @FXML
    private Button addSemesterButton;

    private GradeBookService service;

    @Override
    public void init(AppServices services) {
        this.service = services.gradeBook();

        addSemesterButton.setGraphic(Icons.create(Icons.PLUS, 15));
        addSemesterButton.setOnAction(event -> createSemester());

        service.getGradeBook().getSemesters().addListener(
                (InvalidationListener) observable -> rebuild());
        rebuild();
    }

    private Window window() {
        return bodyBox.getScene() == null ? null : bodyBox.getScene().getWindow();
    }

    private void rebuild() {
        List<Semester> semesters = service.getGradeBook().getSemesters();
        updateSubtitle(semesters);

        bodyBox.getChildren().clear();
        if (semesters.isEmpty()) {
            bodyBox.getChildren().add(createFirstRunCard());
            return;
        }
        semesters.forEach(semester -> bodyBox.getChildren().add(createSemesterCard(semester)));
    }

    private void updateSubtitle(List<Semester> semesters) {
        if (semesters.isEmpty()) {
            subtitleLabel.setText(I18n.t("overview.subtitle.empty"));
            return;
        }
        int modules = (int) service.getGradeBook().countModules();
        int grades = (int) service.getGradeBook().countGrades();
        int insufficient = (int) service.getGradeBook().countInsufficientModules();

        StringBuilder text = new StringBuilder()
                .append(Formats.semesterCount(semesters.size()))
                .append(" · ").append(Formats.moduleCount(modules))
                .append(" · ").append(Formats.gradeCount(grades));
        if (insufficient > 0) {
            text.append(" · ").append(I18n.t("count.insufficientModules", insufficient));
        }
        subtitleLabel.setText(text.toString());
    }

    // ----- Erster Start ---------------------------------------------------

    /** Geführter Einstieg: erklärt den nächsten Schritt und führt ihn auf Klick aus. */
    private Node createFirstRunCard() {
        VBox card = new VBox();
        card.getStyleClass().add("card");

        VBox empty = new VBox();
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);

        HBox mark = new HBox(Icons.create(Icons.CHART, 34));
        mark.getStyleClass().add("empty-state__mark");
        mark.setAlignment(Pos.CENTER);

        Label title = new Label(I18n.t("overview.empty.title"));
        title.getStyleClass().add("empty-state__title");

        Label text = new Label(I18n.t("overview.empty.text"));
        text.getStyleClass().add("empty-state__text");

        Button action = Theme.primary(new Button(I18n.t("overview.empty.action")));
        action.setGraphic(Icons.create(Icons.PLUS, 15));
        action.setOnAction(event -> createSemester());

        HBox actionRow = new HBox(action);
        actionRow.getStyleClass().add("empty-state__action");
        actionRow.setAlignment(Pos.CENTER);

        empty.getChildren().addAll(mark, title, text, actionRow);
        card.getChildren().add(empty);
        return card;
    }

    // ----- Semesterkarte --------------------------------------------------

    private Node createSemesterCard(Semester semester) {
        VBox card = new VBox();
        card.getStyleClass().add("card");
        card.getChildren().add(createCardHeader(semester));

        if (semester.getModules().isEmpty()) {
            card.getChildren().add(createNoModulesState(semester));
            return card;
        }
        List<CourseModule> modules = semester.getModules();
        for (int index = 0; index < modules.size(); index++) {
            if (index > 0) {
                card.getChildren().add(createRowSeparator());
            }
            card.getChildren().add(createModuleRow(modules.get(index)));
        }
        return card;
    }

    private Node createCardHeader(Semester semester) {
        Label name = new Label(semester.getName());
        name.getStyleClass().add("card__title");

        Label meta = new Label(Formats.moduleCount(semester.getModules().size())
                + " · " + I18n.t("count.modulesWithGrade", (int) semester.countGradedModules()));
        meta.getStyleClass().add("card__meta");

        // Der Stempel steht neben dem Namen und sagt es in einem Wort; die
        // Tönung allein wäre für Rot-Grün-Sehschwäche keine Auskunft.
        HBox nameRow = new HBox(name);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        nameRow.setSpacing(10);
        if (semester.isCompleted()) {
            Label stamp = new Label(I18n.t("status.completed"));
            stamp.getStyleClass().addAll("badge", "badge--stamp");
            nameRow.getChildren().add(stamp);
        }

        VBox titleBox = new VBox(nameRow, meta);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button addModule = Theme.secondary(new Button(I18n.t("overview.addModule")));
        addModule.setGraphic(Icons.create(Icons.PLUS, 14));
        addModule.setDisable(semester.isCompleted());
        addModule.setOnAction(event -> createModule(semester));

        HBox header = new HBox(titleBox, createSemesterAverage(semester), addModule);
        header.getStyleClass().add("card__header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node createSemesterAverage(Semester semester) {
        Label caption = new Label(I18n.t("overview.semesterAverage"));
        caption.getStyleClass().add("label-micro");

        BigDecimal average = semester.getAverage();
        Label value = new Label(average == null ? Formats.none() : Formats.exact(average));
        value.getStyleClass().add("grade-value");
        if (average == null) {
            value.getStyleClass().add("grade-value--none");
        }

        Label explanation = new Label(I18n.t(average == null
                ? "overview.semesterAverage.none"
                : "overview.semesterAverage.hint"));
        explanation.getStyleClass().add("grade-exact");

        VBox box = new VBox(caption, value, explanation);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setMinWidth(178);
        return box;
    }

    private Node createNoModulesState(Semester semester) {
        Label text = new Label(I18n.t("overview.noModules.text"));
        text.getStyleClass().add("empty-state__text");

        Button action = Theme.secondary(new Button(I18n.t("overview.noModules.action")));
        action.setGraphic(Icons.create(Icons.PLUS, 14));
        action.setDisable(semester.isCompleted());
        action.setOnAction(event -> createModule(semester));

        VBox empty = new VBox(text, action);
        empty.getStyleClass().addAll("empty-state", "empty-state--compact");
        empty.setAlignment(Pos.CENTER);
        return empty;
    }

    // ----- Modulzeile -----------------------------------------------------

    /**
     * Die Haarlinie zwischen zwei Zeilen.
     *
     * <p>Sie beginnt dort, wo der Text beginnt, und nicht am Kartenrand. Das
     * ist in iOS der Unterschied zwischen «sieht richtig aus» und «fast».</p>
     */
    private static Node createRowSeparator() {
        Region line = new Region();
        line.getStyleClass().add("row-separator");
        HBox.setHgrow(line, Priority.ALWAYS);

        HBox inset = new HBox(line);
        HBox.setMargin(line, new Insets(0, 0, 0, 18));
        return inset;
    }

    private Node createModuleRow(CourseModule module) {
        BigDecimal rounded = module.getRoundedGrade();

        Label name = new Label(module.getName());
        name.getStyleClass().add("module-row__name");

        Label count = new Label(Formats.gradeCount(module.getGrades().size()));
        count.getStyleClass().add("module-row__count");

        VBox nameBox = new VBox(name, count);
        nameBox.setMinWidth(150);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        GradeTrack track = new GradeTrack();
        track.setGrade(rounded);
        track.setPrefWidth(200);
        track.setMinWidth(120);

        Label value = new Label(Formats.grade(rounded));
        value.getStyleClass().add("grade-value");
        if (rounded == null) {
            value.getStyleClass().add("grade-value--none");
        }

        Label exact = new Label(rounded == null ? "" : Formats.exactLabel(module.getExactAverage()));
        exact.getStyleClass().add("grade-exact");

        VBox valueBox = new VBox(value, exact);
        valueBox.setAlignment(Pos.CENTER_RIGHT);
        valueBox.setMinWidth(120);

        Label badge = new Label(Formats.statusText(rounded));
        badge.getStyleClass().addAll("badge", Formats.statusBadgeClass(rounded));

        HBox badgeBox = new HBox(badge);
        badgeBox.setAlignment(Pos.CENTER_RIGHT);
        badgeBox.setMinWidth(112);

        HBox row = new HBox(nameBox, track, valueBox, badgeBox);
        row.getStyleClass().add("module-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setAccessibleText(module.getName() + ", " + Formats.grade(rounded)
                + ", " + Formats.statusText(rounded));
        return row;
    }

    // ----- Aktionen -------------------------------------------------------

    private void createSemester() {
        Dialogs.promptText(window(),
                        I18n.t("dialog.semester.create.title"),
                        I18n.t("dialog.semester.create.hint"),
                        I18n.t("field.semesterName"), "",
                        I18n.t("action.create"),
                        GradeValidator::requireValidText)
                .ifPresent(service::addSemester);
    }

    private void createModule(Semester semester) {
        Dialogs.promptText(window(),
                        I18n.t("dialog.module.create.title"),
                        I18n.t("dialog.module.create.hint", semester.getName()),
                        I18n.t("field.moduleName"), "",
                        I18n.t("action.create"),
                        GradeValidator::requireValidText)
                .ifPresent(name -> {
                    try {
                        service.addModule(semester, name);
                    } catch (ValidationException e) {
                        // Die Schaltfläche ist bei einem abgeschlossenen Semester
                        // ausgegraut; kommt die Änderung trotzdem hier an, soll
                        // der Grund im Klartext erscheinen.
                        Dialogs.error(window(), I18n.t("dialog.locked.title"),
                                I18n.t(e.getMessageKey(), e.getArguments()));
                    }
                });
    }
}
