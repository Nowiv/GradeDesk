package ch.notenrechner.ui.view;

import ch.notenrechner.model.CourseModule;
import ch.notenrechner.model.Semester;
import ch.notenrechner.service.GradeBookService;
import ch.notenrechner.service.GradeValidator;
import ch.notenrechner.ui.AppServices;
import ch.notenrechner.ui.ViewController;
import ch.notenrechner.ui.control.Dialogs;
import ch.notenrechner.ui.control.GradeTrack;
import ch.notenrechner.ui.util.Formats;
import ch.notenrechner.ui.util.Icons;
import ch.notenrechner.ui.util.Theme;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.List;

/**
 * Die Übersicht: pro Semester eine Karte, darin jedes Modul mit Notenband,
 * gerundeter und ungerundeter Note sowie dem Status in Worten.
 *
 * <p>Diese Ansicht ist der Startbildschirm, weil «wie stehe ich gerade?» die
 * häufigste Frage ist. Sie zeigt den Stand ohne dass etwas aufgeklappt werden
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

        service.getGradeBook().getSemesters().addListener((InvalidationListener) observable -> rebuild());
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
            subtitleLabel.setText("Noch keine Daten erfasst.");
            return;
        }
        long modules = service.getGradeBook().countModules();
        long grades = service.getGradeBook().countGrades();
        long insufficient = service.getGradeBook().countInsufficientModules();

        StringBuilder text = new StringBuilder()
                .append(semesters.size() == 1 ? "1 Semester" : semesters.size() + " Semester")
                .append(" · ").append(Formats.moduleCount((int) modules))
                .append(" · ").append(Formats.gradeCount((int) grades));
        if (insufficient > 0) {
            text.append(" · ").append(insufficient == 1
                    ? "1 Modul ungenügend"
                    : insufficient + " Module ungenügend");
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

        Label title = new Label("Mit dem ersten Semester beginnen");
        title.getStyleClass().add("empty-state__title");

        Label text = new Label("""
                Notenrechner ist noch leer. Lege zuerst ein Semester an, darin die \
                Module und darin die einzelnen Noten. Modulnote und Semesterschnitt \
                rechnet die Anwendung von selbst, und jede Änderung wird sofort \
                gespeichert.""");
        text.getStyleClass().add("empty-state__text");

        Button action = Theme.primary(new Button("Erstes Semester anlegen"));
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
        semester.getModules().forEach(module -> card.getChildren().add(createModuleRow(module)));
        return card;
    }

    private Node createCardHeader(Semester semester) {
        Label name = new Label(semester.getName());
        name.getStyleClass().add("card__title");

        Label meta = new Label(Formats.moduleCount(semester.getModules().size())
                + " · " + semester.countGradedModules() + " davon mit Note");
        meta.getStyleClass().add("card__meta");

        VBox titleBox = new VBox(name, meta);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button addModule = Theme.secondary(new Button("Modul hinzufügen"));
        addModule.setGraphic(Icons.create(Icons.PLUS, 14));
        addModule.setOnAction(event -> createModule(semester));

        HBox header = new HBox(titleBox, createSemesterAverage(semester), addModule);
        header.getStyleClass().add("card__header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node createSemesterAverage(Semester semester) {
        Label caption = new Label("SEMESTERSCHNITT");
        caption.getStyleClass().add("label-micro");

        BigDecimal average = semester.getAverage();
        Label value = new Label(average == null ? Formats.NO_VALUE : Formats.exact(average));
        value.getStyleClass().add("grade-value");
        if (average == null) {
            value.getStyleClass().add("grade-value--none");
        }

        Label explanation = new Label(average == null
                ? "noch keine Modulnote"
                : "aus den gerundeten Modulnoten");
        explanation.getStyleClass().add("grade-exact");

        VBox box = new VBox(caption, value, explanation);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setMinWidth(170);
        return box;
    }

    private Node createNoModulesState(Semester semester) {
        Label text = new Label("In diesem Semester ist noch kein Modul erfasst.");
        text.getStyleClass().add("empty-state__text");

        Button action = Theme.secondary(new Button("Erstes Modul anlegen"));
        action.setGraphic(Icons.create(Icons.PLUS, 14));
        action.setOnAction(event -> createModule(semester));

        VBox empty = new VBox(text, action);
        empty.getStyleClass().addAll("empty-state", "empty-state--compact");
        empty.setAlignment(Pos.CENTER);
        return empty;
    }

    // ----- Modulzeile -----------------------------------------------------

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
        valueBox.setMinWidth(116);

        Label badge = new Label(Formats.statusText(rounded));
        badge.getStyleClass().addAll("badge", Formats.statusBadgeClass(rounded));

        HBox badgeBox = new HBox(badge);
        badgeBox.setAlignment(Pos.CENTER_RIGHT);
        badgeBox.setMinWidth(108);

        HBox row = new HBox(nameBox, track, valueBox, badgeBox);
        row.getStyleClass().add("module-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setAccessibleText(module.getName() + ", Modulnote " + Formats.grade(rounded)
                + ", " + Formats.statusText(rounded));
        return row;
    }

    // ----- Aktionen -------------------------------------------------------

    private void createSemester() {
        Dialogs.promptText(window(), "Semester anlegen",
                        "Zum Beispiel «HS 2025» oder «1. Semester».",
                        "Semestername", "", "Anlegen",
                        text -> GradeValidator.requireValidText(text, "Semestername"))
                .ifPresent(service::addSemester);
    }

    private void createModule(Semester semester) {
        Dialogs.promptText(window(), "Modul anlegen",
                        "Das Modul gehört zum Semester «" + semester.getName() + "».",
                        "Modulname", "", "Anlegen",
                        text -> GradeValidator.requireValidText(text, "Modulname"))
                .ifPresent(name -> service.addModule(semester, name));
    }
}
