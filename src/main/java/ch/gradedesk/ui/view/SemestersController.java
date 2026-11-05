package ch.gradedesk.ui.view;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.model.CourseModule;
import ch.gradedesk.model.Grade;
import ch.gradedesk.model.Semester;
import ch.gradedesk.service.GradeBookService;
import ch.gradedesk.service.GradeValidator;
import ch.gradedesk.ui.AppServices;
import ch.gradedesk.ui.ViewController;
import ch.gradedesk.ui.control.Dialogs;
import ch.gradedesk.ui.control.GradeDialog;
import ch.gradedesk.ui.control.GradeTrack;
import ch.gradedesk.ui.util.Formats;
import ch.gradedesk.ui.util.Icons;
import javafx.beans.InvalidationListener;
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
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
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
    private Button addSemesterButton;
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
                        : " · " + Formats.averageLabel(semester.getAverage()))));
        semesterList.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> onSemesterSelected(selected));

        iconButton(renameSemesterButton, Icons.EDIT, I18n.t("semesters.renameSemester"));
        iconButton(deleteSemesterButton, Icons.DELETE, I18n.t("semesters.deleteSemester"));

        addSemesterButton.setGraphic(Icons.create(Icons.PLUS, 14));
        addSemesterButton.setOnAction(event -> createSemester());
        renameSemesterButton.setOnAction(event -> renameSemester());
        deleteSemesterButton.setOnAction(event -> deleteSemester());

        semesterList.setPlaceholder(placeholder(I18n.t("semesters.placeholder.noSemester")));
    }

    private void onSemesterSelected(Semester semester) {
        boolean present = semester != null;
        renameSemesterButton.setDisable(!present);
        deleteSemesterButton.setDisable(!present);
        addModuleButton.setDisable(!present);

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
                .ifPresent(name -> {
                    service.renameSemester(semester, name);
                    semesterList.refresh();
                    refreshMeta();
                });
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
            service.removeSemester(semester);
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
                .ifPresent(name -> moduleList.getSelectionModel().select(
                        service.addModule(semester, name)));
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
                .ifPresent(name -> {
                    service.renameModule(module, name);
                    moduleList.refresh();
                    gradesTitleLabel.setText(module.getName());
                    refreshMeta();
                });
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
            service.removeModule(semester, module);
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
        input.ifPresent(result -> {
            service.addGrade(module, result.label(), result.value(), result.weight(), result.date());
            moduleList.refresh();
            refreshMeta();
        });
    }

    private void editGrade(Grade grade) {
        CourseModule module = moduleList.getSelectionModel().getSelectedItem();
        if (module == null || grade == null) {
            return;
        }
        GradeDialog.forExistingGrade(window(), module.getName(), grade).ifPresent(result -> {
            service.updateGrade(grade, result.label(), result.value(), result.weight(), result.date());
            gradesTable.refresh();
            moduleList.refresh();
            refreshMeta();
        });
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
            service.removeGrade(module, grade);
            moduleList.refresh();
            refreshMeta();
        }
    }

    // ----- Gemeinsames ------------------------------------------------------

    /** Hält die Kopfzeilen der drei Spalten mit dem aktuellen Stand im Takt. */
    private void refreshMeta() {
        int semesterCount = service.getGradeBook().getSemesters().size();
        semesterMetaLabel.setText(Formats.semesterCount(semesterCount));

        Semester semester = semesterList.getSelectionModel().getSelectedItem();
        moduleMetaLabel.setText(semester == null
                ? I18n.t("semesters.noSemesterSelected")
                : I18n.t("semesters.inSemester", semester.getName()));

        CourseModule module = moduleList.getSelectionModel().getSelectedItem();
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
        private final Label title = new Label();
        private final Label meta = new Label();
        private final VBox box = new VBox(title, meta);

        private PickerCell(java.util.function.Function<T, String> titleOf,
                           java.util.function.Function<T, String> metaOf) {
            this.titleOf = titleOf;
            this.metaOf = metaOf;
            title.getStyleClass().add("picker-cell__title");
            meta.getStyleClass().add("picker-cell__meta");
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
            box.getStyleClass().remove("picker-cell--selected");
            if (isSelected()) {
                box.getStyleClass().add("picker-cell--selected");
            }
            setGraphic(box);
            setAccessibleText(title.getText() + ", " + meta.getText());
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
            edit.setOnAction(event -> editGrade(grade));
            delete.setOnAction(event -> deleteGrade(grade));
            setGraphic(box);
        }
    }
}
