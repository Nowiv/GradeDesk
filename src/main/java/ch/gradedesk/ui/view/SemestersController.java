package ch.gradedesk.ui.view;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.model.CourseModule;
import ch.gradedesk.model.Grade;
import ch.gradedesk.model.PlannedExam;
import ch.gradedesk.model.Semester;
import ch.gradedesk.service.GradeBookService;
import ch.gradedesk.service.GradeValidator;
import ch.gradedesk.service.ValidationException;
import ch.gradedesk.ui.AppServices;
import ch.gradedesk.ui.ViewController;
import ch.gradedesk.ui.control.Dialogs;
import ch.gradedesk.ui.control.GradeDialog;
import ch.gradedesk.ui.control.GradeTrack;
import ch.gradedesk.ui.control.PlannedExamDialog;
import ch.gradedesk.ui.util.Formats;
import ch.gradedesk.ui.util.Icons;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Die Arbeitsansicht: Semester, Module und Noten anlegen, ändern und löschen.
 *
 * <p>Drei Spalten, von links nach rechts immer konkreter. Die Auswahl links
 * bestimmt, was rechts erscheint – ein Muster, das aus Dateimanagern und
 * Mailprogrammen vertraut ist und deshalb nicht erklärt werden muss.</p>
 */
public final class SemestersController implements ViewController {

    @FXML
    private ListView<Semester> semesterList;
    @FXML
    private ListView<CourseModule> moduleList;
    @FXML
    private TableView<Grade> gradesTable;

    @FXML
    private TableColumn<Grade, String> labelColumn;
    @FXML
    private TableColumn<Grade, String> dateColumn;
    @FXML
    private TableColumn<Grade, String> valueColumn;
    @FXML
    private TableColumn<Grade, String> weightColumn;
    @FXML
    private TableColumn<Grade, Grade> actionColumn;

    @FXML
    private Label semesterMetaLabel;
    @FXML
    private Label moduleMetaLabel;
    @FXML
    private Label gradesTitleLabel;
    @FXML
    private Label gradesMetaLabel;
    @FXML
    private HBox moduleGradeBox;
    @FXML
    private VBox plannedExamBox;

    @FXML
    private Button addSemesterButton;
    @FXML
    private Button completeSemesterButton;
    @FXML
    private Button renameSemesterButton;
    @FXML
    private Button deleteSemesterButton;
    @FXML
    private Button addModuleButton;
    @FXML
    private Button renameModuleButton;
    @FXML
    private Button deleteModuleButton;
    @FXML
    private Button addGradeButton;

    private GradeBookService service;
    private final GradeTrack moduleTrack = new GradeTrack();
    private final Label moduleGradeLabel = new Label();
    private final Label moduleExactLabel = new Label();

    @Override
    public void init(AppServices services) {
        this.service = services.gradeBook();

        // Erst alle drei Spalten verdrahten, dann auswaehlen. Andernfalls liefe die
        // erste Auswahl los, bevor die nachgelagerten Listener angemeldet sind.
        setUpSemesterColumn();
        setUpModuleColumn();
        setUpGradeColumn();

        service.getGradeBook().getSemesters().addListener(
                (InvalidationListener) observable -> refreshMeta());

        if (semesterList.getItems().isEmpty()) {
            onSemesterSelected(null);
        } else {
            semesterList.getSelectionModel().selectFirst();
        }
        refreshMeta();
    }

    @Override
    public void onShown() {
        if (semesterList.getSelectionModel().isEmpty() && !semesterList.getItems().isEmpty()) {
            semesterList.getSelectionModel().selectFirst();
        }
    }

    private Window window() {
        return semesterList.getScene() == null ? null : semesterList.getScene().getWindow();
    }

    // ----- Spalte 1: Semester ---------------------------------------------

    private void setUpSemesterColumn() {
        semesterList.setItems(service.getGradeBook().getSemesters());
        semesterList.setCellFactory(view -> new PickerCell<>(
                Semester::getName,
                semester -> Formats.moduleCount(semester.getModules().size())
                        + (semester.getAverage() == null
                        ? ""
                        : " · " + Formats.averageLabel(semester.getAverage())),
                semester -> semester.isCompleted() ? I18n.t("status.completed") : null));
        semesterList.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> onSemesterSelected(selected));

        iconButton(renameSemesterButton, Icons.EDIT, I18n.t("semesters.renameSemester"));
        iconButton(deleteSemesterButton, Icons.DELETE, I18n.t("semesters.deleteSemester"));
        iconButton(completeSemesterButton, Icons.LOCK, I18n.t("semesters.completeSemester"));

        addSemesterButton.setGraphic(Icons.create(Icons.PLUS, 14));
        addSemesterButton.setOnAction(event -> createSemester());
        renameSemesterButton.setOnAction(event -> renameSemester());
        deleteSemesterButton.setOnAction(event -> deleteSemester());
        completeSemesterButton.setOnAction(event -> toggleCompleted());

        semesterList.setPlaceholder(placeholder(I18n.t("semesters.placeholder.noSemester")));
    }

    private void onSemesterSelected(Semester semester) {
        boolean present = semester != null;
        completeSemesterButton.setDisable(!present);

        moduleList.setItems(present
                ? semester.getModules()
                : FXCollections.observableArrayList());
        moduleList.getSelectionModel().clearSelection();
        if (present && !semester.getModules().isEmpty()) {
            moduleList.getSelectionModel().selectFirst();
        } else {
            onModuleSelected(null);
        }
        refreshMeta();
    }

    /**
     * Schliesst das gewählte Semester ab oder öffnet es wieder.
     *
     * <p>Abschliessen geschieht ohne Rückfrage: es ist in zwei Klicks
     * rückgängig gemacht und bis dahin deutlich sichtbar. Das Wiederöffnen
     * dagegen hebt eine Sperre auf und wird deshalb erklärt und bestätigt.</p>
     */
    private void toggleCompleted() {
        Semester semester = semesterList.getSelectionModel().getSelectedItem();
        if (semester == null) {
            return;
        }
        if (!semester.isCompleted()) {
            service.completeSemester(semester);
        } else if (Dialogs.confirm(window(), I18n.t("dialog.reopen.title"),
                I18n.t("dialog.reopen.text", semester.getName()),
                I18n.t("dialog.reopen.confirm"))) {
            service.reopenSemester(semester);
        } else {
            return;
        }
        semesterList.refresh();
        refreshMeta();
    }

    private void createSemester() {
        Dialogs.promptText(window(), I18n.t("dialog.semester.create.title"),
                        I18n.t("dialog.semester.create.hint"),
                        I18n.t("field.semesterName"), "",
                        I18n.t("action.create"),
                        GradeValidator::requireValidText)
                .ifPresent(name -> semesterList.getSelectionModel().select(service.addSemester(name)));
    }

    private void renameSemester() {
        Semester semester = semesterList.getSelectionModel().getSelectedItem();
        if (semester == null) {
            return;
        }
        Dialogs.promptText(window(), I18n.t("dialog.semester.rename.title"), null,
                        I18n.t("field.semesterName"), semester.getName(),
                        I18n.t("action.save"),
                        GradeValidator::requireValidText)
                .ifPresent(name -> guarded(() -> {
                    service.renameSemester(semester, name);
                    semesterList.refresh();
                    refreshMeta();
                }));
    }

    private void deleteSemester() {
        Semester semester = semesterList.getSelectionModel().getSelectedItem();
        if (semester == null) {
            return;
        }
        long grades = semester.getModules().stream().mapToLong(m -> m.getGrades().size()).sum();
        String consequence = I18n.t("dialog.delete.semester.text", semester.getName(),
                Formats.moduleCount(semester.getModules().size()), Formats.gradeCount((int) grades));

        if (Dialogs.confirmDelete(window(), I18n.t("dialog.delete.semester.title"), consequence,
                I18n.t("semesters.deleteSemester"))) {
            guarded(() -> service.removeSemester(semester));
        }
    }

    // ----- Spalte 2: Module -----------------------------------------------

    private void setUpModuleColumn() {
        moduleList.setCellFactory(view -> new PickerCell<>(
                CourseModule::getName,
                module -> Formats.gradeCount(module.getGrades().size())
                        + (module.hasGrade() ? " · " + Formats.grade(module.getRoundedGrade()) : "")));
        moduleList.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> onModuleSelected(selected));

        iconButton(renameModuleButton, Icons.EDIT, I18n.t("semesters.renameModule"));
        iconButton(deleteModuleButton, Icons.DELETE, I18n.t("semesters.deleteModule"));

        addModuleButton.setGraphic(Icons.create(Icons.PLUS, 14));
        addModuleButton.setOnAction(event -> createModule());
        renameModuleButton.setOnAction(event -> renameModule());
        deleteModuleButton.setOnAction(event -> deleteModule());

        moduleList.setPlaceholder(placeholder(I18n.t("semesters.placeholder.noModule")));
    }

    private void onModuleSelected(CourseModule module) {
        boolean present = module != null;
        renameModuleButton.setDisable(!present);
        deleteModuleButton.setDisable(!present);
        addGradeButton.setDisable(!present);

        gradesTable.setItems(present ? module.getGrades() : FXCollections.observableArrayList());
        gradesTitleLabel.setText(present ? module.getName() : I18n.t("semesters.grades"));
        refreshMeta();
    }

    private void createModule() {
        Semester semester = semesterList.getSelectionModel().getSelectedItem();
        if (semester == null) {
            return;
        }
        Dialogs.promptText(window(), I18n.t("dialog.module.create.title"),
                        I18n.t("dialog.module.create.hint", semester.getName()),
                        I18n.t("field.moduleName"), "",
                        I18n.t("action.create"),
                        GradeValidator::requireValidText)
                .ifPresent(name -> guarded(() -> moduleList.getSelectionModel().select(
                        service.addModule(semester, name))));
    }

    private void renameModule() {
        CourseModule module = moduleList.getSelectionModel().getSelectedItem();
        if (module == null) {
            return;
        }
        Dialogs.promptText(window(), I18n.t("dialog.module.rename.title"), null,
                        I18n.t("field.moduleName"), module.getName(),
                        I18n.t("action.save"),
                        GradeValidator::requireValidText)
                .ifPresent(name -> guarded(() -> {
                    service.renameModule(module, name);
                    moduleList.refresh();
                    gradesTitleLabel.setText(module.getName());
                    refreshMeta();
                }));
    }

    private void deleteModule() {
        Semester semester = semesterList.getSelectionModel().getSelectedItem();
        CourseModule module = moduleList.getSelectionModel().getSelectedItem();
        if (semester == null || module == null) {
            return;
        }
        String consequence = I18n.t("dialog.delete.module.text", module.getName(),
                Formats.gradeCount(module.getGrades().size()));

        if (Dialogs.confirmDelete(window(), I18n.t("dialog.delete.module.title"), consequence,
                I18n.t("semesters.deleteModule"))) {
            guarded(() -> service.removeModule(semester, module));
        }
    }

    // ----- Spalte 3: Noten -------------------------------------------------

    private void setUpGradeColumn() {
        labelColumn.setCellValueFactory(data -> data.getValue().labelProperty());
        dateColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(Formats.date(data.getValue().getDate())));
        valueColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(Formats.gradeInput(data.getValue().getValue())));
        weightColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(Formats.weightFactor(data.getValue().getWeight())));
        actionColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));

        // Zahlen stehen rechtsbündig, damit die Spalten untereinander fluchten.
        valueColumn.getStyleClass().add("table-cell--numeric");
        weightColumn.getStyleClass().add("table-cell--numeric");
        valueColumn.setCellFactory(column -> numericCell());
        weightColumn.setCellFactory(column -> numericCell());
        actionColumn.setCellFactory(column -> new ActionCell());

        gradesTable.setPlaceholder(placeholder(I18n.t("semesters.placeholder.noGrade")));
        gradesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        gradesTable.setRowFactory(table -> {
            javafx.scene.control.TableRow<Grade> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    editGrade(row.getItem());
                }
            });
            return row;
        });

        addGradeButton.setGraphic(Icons.create(Icons.PLUS, 14));
        addGradeButton.setOnAction(event -> createGrade());

        moduleGradeLabel.getStyleClass().add("grade-value");
        moduleExactLabel.getStyleClass().add("grade-exact");
        moduleTrack.setMaxWidth(Double.MAX_VALUE);
        moduleTrack.setMinWidth(110);

        VBox numbers = new VBox(moduleGradeLabel, moduleExactLabel);
        numbers.setAlignment(Pos.CENTER_RIGHT);
        numbers.setMinWidth(104);

        HBox.setHgrow(moduleTrack, Priority.ALWAYS);
        moduleGradeBox.getChildren().addAll(moduleTrack, numbers);
    }

    private void createGrade() {
        CourseModule module = moduleList.getSelectionModel().getSelectedItem();
        if (module == null) {
            return;
        }
        Optional<GradeDialog.Result> input = GradeDialog.forNewGrade(window(), module.getName());
        input.ifPresent(result -> guarded(() -> {
            service.addGrade(module, result.label(), result.value(), result.weight(), result.date());
            moduleList.refresh();
            refreshMeta();
        }));
    }

    private void editGrade(Grade grade) {
        CourseModule module = moduleList.getSelectionModel().getSelectedItem();
        // Hierher führt auch der Doppelklick auf eine Zeile, und der lässt sich
        // nicht ausgrauen. Ist das Semester abgeschlossen, soll deshalb gar kein
        // Formular aufgehen, statt die Eingabe am Ende abzuweisen.
        if (module == null || grade == null || isLocked()) {
            return;
        }
        GradeDialog.forExistingGrade(window(), module.getName(), grade).ifPresent(result -> guarded(() -> {
            service.updateGrade(grade, result.label(), result.value(), result.weight(), result.date());
            gradesTable.refresh();
            moduleList.refresh();
            refreshMeta();
        }));
    }

    private void deleteGrade(Grade grade) {
        CourseModule module = moduleList.getSelectionModel().getSelectedItem();
        if (module == null || grade == null) {
            return;
        }
        String consequence = I18n.t("dialog.delete.grade.text", grade.getLabel(),
                Formats.gradeInput(grade.getValue()), Formats.weightFactor(grade.getWeight()),
                module.getName());

        if (Dialogs.confirmDelete(window(), I18n.t("dialog.delete.grade.title"), consequence,
                I18n.t("semesters.deleteGrade"))) {
            guarded(() -> {
                service.removeGrade(module, grade);
                moduleList.refresh();
                refreshMeta();
            });
        }
    }

    // ----- Geplante Prüfungen ----------------------------------------------

    /**
     * Baut die Zeile der geplanten Prüfungen neu auf.
     *
     * <p>Sie steht zwischen Modulnote und Notentabelle, also genau dort, wo die
     * Noten erfasst werden. Ohne geplante Prüfung bleibt nur ein leiser Link
     * stehen; die Zeile verschwindet nie ganz, damit der Weg dorthin bekannt
     * bleibt.</p>
     */
    private void refreshPlannedExams(CourseModule module) {
        plannedExamBox.getChildren().clear();
        boolean present = module != null;
        plannedExamBox.setVisible(present);
        plannedExamBox.setManaged(present);
        if (!present) {
            return;
        }

        List<PlannedExam> exams = module.plannedExamsByDate();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < exams.size(); i++) {
            plannedExamBox.getChildren().add(plannedExamRow(module, exams.get(i), i == 0, today));
        }
        plannedExamBox.getChildren().add(addExamRow(module, exams.isEmpty()));
    }

    /**
     * Eine Zeile je geplanter Prüfung; die erste ist die nächste anstehende.
     *
     * <p>Zwei Textzeilen: oben der Termin, darunter Stand, Bezeichnung und
     * Gewichtung. Alles nebeneinander ginge sich in einem schmalen Fenster
     * nicht aus, und dann verschwände ausgerechnet der Termin. So behält die
     * obere Zeile die ganze verfügbare Breite für sich.</p>
     */
    private Node plannedExamRow(CourseModule module, PlannedExam exam, boolean next,
                                LocalDate today) {
        boolean overdue = exam.isOverdue(today);
        String when = Formats.dateWithWeekday(exam.getDate());
        String line = next ? I18n.t("plannedExam.next", when) : when;
        if (!overdue) {
            line = line + " · " + Formats.examDue(exam.getDate(), today);
        }

        Label date = new Label(line);
        date.getStyleClass().add(next ? "planned-exam__next" : "planned-exam__when");
        // Kein Umbruch: die Zeilen stehen in einem VBox mit fester Zeilenhoehe,
        // ein wachsendes Label wuerde die naechste Zeile ueberdecken. In einem
        // sehr schmalen Fenster kuerzt JavaFX den Text; der volle Wortlaut
        // steht dann im Hinweis weiter unten.
        date.setMinWidth(0);

        VBox text = new VBox(date);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox second = new HBox();
        second.getStyleClass().add("planned-exam__line");
        second.setAlignment(Pos.CENTER_LEFT);
        second.setMinWidth(0);

        // Ist der Termin vorbei und fehlt die Note noch, wird das benannt und
        // nicht stillschweigend ausgeblendet. Das Wort steht neben der Farbe,
        // und es wird nie gekürzt.
        if (overdue) {
            Label pending = new Label(I18n.t("plannedExam.pending"));
            pending.getStyleClass().addAll("badge", "badge--warn");
            pending.setMinWidth(Region.USE_PREF_SIZE);
            second.getChildren().add(pending);
        }

        String detail = exam.hasLabel() ? exam.getLabel() : "";
        if (exam.getWeight().compareTo(PlannedExam.DEFAULT_WEIGHT) != 0) {
            detail = detail.isEmpty()
                    ? Formats.weightFactor(exam.getWeight())
                    : detail + " · " + Formats.weightFactor(exam.getWeight());
        }
        if (!detail.isEmpty()) {
            Label label = new Label(detail);
            label.getStyleClass().add("planned-exam__detail");
            label.setMinWidth(0);
            second.getChildren().add(label);
        }
        if (!second.getChildren().isEmpty()) {
            text.getChildren().add(second);
        }

        boolean locked = isLocked();

        Button enterGrade = new Button(I18n.t("plannedExam.enterGrade"));
        enterGrade.getStyleClass().addAll("btn", "btn-tinted", "btn--compact");
        enterGrade.setMinWidth(Region.USE_PREF_SIZE);
        enterGrade.setDisable(locked);
        enterGrade.setOnAction(event -> enterGradeFor(module, exam));

        Button edit = new Button();
        edit.getStyleClass().add("btn-quiet-icon");
        iconButton(edit, Icons.EDIT, I18n.t("plannedExam.edit"));
        edit.setDisable(locked);
        edit.setOnAction(event -> editPlannedExam(module, exam));

        Button delete = new Button();
        delete.getStyleClass().addAll("btn-quiet-icon", "btn-quiet-icon--danger");
        iconButton(delete, Icons.DELETE, I18n.t("plannedExam.delete"));
        delete.setDisable(locked);
        delete.setOnAction(event -> deletePlannedExam(module, exam));

        HBox actions = new HBox(enterGrade, edit, delete);
        actions.getStyleClass().add("planned-exam__actions");
        actions.setMinWidth(Region.USE_PREF_SIZE);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(text, actions);
        row.getStyleClass().add("planned-exam");
        row.setAlignment(Pos.CENTER_LEFT);

        String full = line
                + (overdue ? " · " + I18n.t("plannedExam.pending") : "")
                + (detail.isEmpty() ? "" : " · " + detail);
        row.setAccessibleText(full);
        Tooltip.install(row, new Tooltip(full));
        return row;
    }

    /** Der zurückhaltende Link zum Eintragen einer weiteren Prüfung. */
    private Node addExamRow(CourseModule module, boolean alone) {
        Button add = new Button(I18n.t("plannedExam.add"));
        add.getStyleClass().addAll("btn", "btn-plain", "btn--compact", "planned-exam__add");
        add.setGraphic(Icons.create(Icons.PLUS, 13));
        add.setDisable(isLocked());
        add.setOnAction(event -> createPlannedExam(module));

        HBox row = new HBox(add);
        row.getStyleClass().add(alone ? "planned-exam--empty" : "planned-exam");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void createPlannedExam(CourseModule module) {
        PlannedExamDialog.forNewExam(window(), module.getName()).ifPresent(result -> guarded(() -> {
            service.addPlannedExam(module, result.label(), result.weight(), result.date());
            refreshMeta();
        }));
    }

    private void editPlannedExam(CourseModule module, PlannedExam exam) {
        PlannedExamDialog.forExistingExam(window(), module.getName(), exam).ifPresent(result -> guarded(() -> {
            service.updatePlannedExam(exam, result.label(), result.weight(), result.date());
            refreshMeta();
        }));
    }

    private void deletePlannedExam(CourseModule module, PlannedExam exam) {
        String consequence = I18n.t("dialog.delete.plannedExam.text",
                Formats.date(exam.getDate()), module.getName());

        if (Dialogs.confirmDelete(window(), I18n.t("dialog.delete.plannedExam.title"),
                consequence, I18n.t("plannedExam.delete"))) {
            guarded(() -> {
                service.removePlannedExam(module, exam);
                refreshMeta();
            });
        }
    }

    /**
     * Die Prüfung ist zurück: das Notenformular steht mit Bezeichnung,
     * Gewichtung und Datum bereit. Wird die Note gespeichert, verschwindet die
     * geplante Prüfung; bricht man ab, bleibt sie stehen.
     */
    private void enterGradeFor(CourseModule module, PlannedExam exam) {
        GradeDialog.forPlannedExam(window(), module.getName(), exam).ifPresent(result -> guarded(() -> {
            service.convertToGrade(module, exam, result.label(), result.value(),
                    result.weight(), result.date());
            moduleList.refresh();
            refreshMeta();
        }));
    }

    // ----- Gemeinsames ------------------------------------------------------

    /** Hält die Kopfzeilen der drei Spalten mit dem aktuellen Stand im Takt. */
    private void refreshMeta() {
        int semesterCount = service.getGradeBook().getSemesters().size();
        semesterMetaLabel.setText(Formats.semesterCount(semesterCount));

        Semester semester = semesterList.getSelectionModel().getSelectedItem();
        refreshLockState(semester);
        moduleMetaLabel.setText(semester == null
                ? I18n.t("semesters.noSemesterSelected")
                : I18n.t("semesters.inSemester", semester.getName()));

        CourseModule module = moduleList.getSelectionModel().getSelectedItem();
        refreshPlannedExams(module);
        if (module == null) {
            gradesMetaLabel.setText(I18n.t("semesters.noModuleSelected"));
            moduleTrack.setGrade(null);
            moduleGradeLabel.setText(Formats.none());
            moduleExactLabel.setText("");
            moduleGradeBox.setVisible(false);
            moduleGradeBox.setManaged(false);
            return;
        }

        moduleGradeBox.setVisible(true);
        moduleGradeBox.setManaged(true);
        BigDecimal rounded = module.getRoundedGrade();
        gradesMetaLabel.setText(Formats.gradeCount(module.getGrades().size())
                + " · " + Formats.statusText(rounded));
        moduleTrack.setGrade(rounded);
        moduleGradeLabel.setText(Formats.grade(rounded));
        moduleGradeLabel.getStyleClass().remove("grade-value--none");
        if (rounded == null) {
            moduleGradeLabel.getStyleClass().add("grade-value--none");
        }
        moduleExactLabel.setText(rounded == null ? "" : Formats.exactLabel(module.getExactAverage()));
    }

    /** {@code true}, wenn das gewählte Semester abgeschlossen und damit gesperrt ist. */
    private boolean isLocked() {
        Semester semester = semesterList.getSelectionModel().getSelectedItem();
        return semester != null && semester.isCompleted();
    }

    /**
     * Führt die Schaltflächen am Sperrzustand nach.
     *
     * <p>Ausgegraut heisst hier: die Person sieht sofort, dass hier nichts geht.
     * Verhindert wird es im Service – diese Zeilen sind die Erklärung, nicht
     * die Sperre.</p>
     */
    private void refreshLockState(Semester semester) {
        boolean present = semester != null;
        boolean open = present && !semester.isCompleted();

        completeSemesterButton.setDisable(!present);
        iconButton(completeSemesterButton,
                present && semester.isCompleted() ? Icons.UNLOCK : Icons.LOCK,
                I18n.t(present && semester.isCompleted()
                        ? "semesters.reopenSemester"
                        : "semesters.completeSemester"));

        renameSemesterButton.setDisable(!open);
        deleteSemesterButton.setDisable(!open);
        addModuleButton.setDisable(!open);

        boolean moduleSelected = moduleList.getSelectionModel().getSelectedItem() != null;
        renameModuleButton.setDisable(!open || !moduleSelected);
        deleteModuleButton.setDisable(!open || !moduleSelected);
        addGradeButton.setDisable(!open || !moduleSelected);
        gradesTable.refresh();
    }

    /**
     * Führt eine Änderung aus und zeigt eine abgewiesene Änderung als Meldung.
     *
     * <p>Im Normalfall greift die Sperre nie bis hierher, weil die Schaltfläche
     * ausgegraut ist. Falls doch – etwa über eine Tastenkombination oder einen
     * Pfad, an den hier niemand gedacht hat –, erscheint der Grund im Klartext
     * statt gar nichts.</p>
     */
    private void guarded(Runnable action) {
        try {
            action.run();
        } catch (ValidationException e) {
            Dialogs.error(window(), I18n.t("dialog.locked.title"),
                    I18n.t(e.getMessageKey(), e.getArguments()));
        }
    }

    /** Tabellenzelle für eine Zahl: gleiche Darstellung, nur rechtsbündig. */
    private static TableCell<Grade, String> numericCell() {
        TableCell<Grade, String> cell = new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
            }
        };
        cell.getStyleClass().add("table-cell--numeric");
        return cell;
    }

    private static void iconButton(Button button, String iconPath, String tooltip) {
        button.setGraphic(Icons.create(iconPath, 17));
        button.setTooltip(new Tooltip(tooltip));
        button.setAccessibleText(tooltip);
    }

    private static Node placeholder(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("label-muted");
        return label;
    }

    /** Listeneintrag mit Titel und leiser Zusatzzeile. */
    private static final class PickerCell<T> extends ListCell<T> {

        private final java.util.function.Function<T, String> titleOf;
        private final java.util.function.Function<T, String> metaOf;
        private final java.util.function.Function<T, String> badgeOf;
        private final Label title = new Label();
        private final Label meta = new Label();
        private final Label badge = new Label();
        private final HBox titleRow = new HBox(title, badge);
        private final VBox box = new VBox(titleRow, meta);

        private PickerCell(java.util.function.Function<T, String> titleOf,
                           java.util.function.Function<T, String> metaOf) {
            this(titleOf, metaOf, item -> null);
        }

        private PickerCell(java.util.function.Function<T, String> titleOf,
                           java.util.function.Function<T, String> metaOf,
                           java.util.function.Function<T, String> badgeOf) {
            this.titleOf = titleOf;
            this.metaOf = metaOf;
            this.badgeOf = badgeOf;
            title.getStyleClass().add("picker-cell__title");
            meta.getStyleClass().add("picker-cell__meta");
            badge.getStyleClass().addAll("badge", "badge--stamp");
            badge.setMinWidth(Region.USE_PREF_SIZE);
            titleRow.getStyleClass().add("picker-cell__title-row");
            titleRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(title, Priority.ALWAYS);
            title.setMinWidth(0);

            // Der Eintrag darf nie breiter sein als die Liste: sonst ragt der
            // Stempel über den Kartenrand hinaus und wird mitten im Wort
            // abgeschnitten. So weicht zuerst der Name, der Stempel bleibt ganz.
            box.setMinWidth(0);
            box.prefWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.max(0, getWidth() - getInsets().getLeft() - getInsets().getRight()),
                    widthProperty(), insetsProperty()));
            box.getStyleClass().add("picker-cell");
        }

        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setAccessibleText(null);
                return;
            }
            title.setText(titleOf.apply(item));
            meta.setText(metaOf.apply(item));

            String stamp = badgeOf.apply(item);
            badge.setText(stamp == null ? "" : stamp);
            badge.setVisible(stamp != null);
            badge.setManaged(stamp != null);

            box.getStyleClass().remove("picker-cell--selected");
            if (isSelected()) {
                box.getStyleClass().add("picker-cell--selected");
            }
            setGraphic(box);
            setAccessibleText(title.getText() + ", " + meta.getText()
                    + (stamp == null ? "" : ", " + stamp));
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            box.getStyleClass().remove("picker-cell--selected");
            if (selected) {
                box.getStyleClass().add("picker-cell--selected");
            }
        }
    }

    /** Tabellenzelle mit den beiden Aktionen pro Note. */
    private final class ActionCell extends TableCell<Grade, Grade> {

        private final Button edit = new Button();
        private final Button delete = new Button();
        private final HBox box = new HBox(edit, delete);

        private ActionCell() {
            edit.getStyleClass().add("btn-quiet-icon");
            delete.getStyleClass().addAll("btn-quiet-icon", "btn-quiet-icon--danger");
            iconButton(edit, Icons.EDIT, I18n.t("semesters.editGrade"));
            iconButton(delete, Icons.DELETE, I18n.t("semesters.deleteGrade"));
            box.setSpacing(2);
            box.setAlignment(Pos.CENTER_RIGHT);
        }

        @Override
        protected void updateItem(Grade grade, boolean empty) {
            super.updateItem(grade, empty);
            if (empty || grade == null) {
                setGraphic(null);
                return;
            }
            boolean locked = isLocked();
            edit.setDisable(locked);
            delete.setDisable(locked);
            edit.setOnAction(event -> editGrade(grade));
            delete.setOnAction(event -> deleteGrade(grade));
            setGraphic(box);
        }
    }
}
