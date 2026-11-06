package ch.gradedesk.ui.control;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.model.PlannedExam;
import ch.gradedesk.service.GradeValidator;
import ch.gradedesk.ui.util.Formats;
import javafx.application.Platform;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Formular für eine geplante Prüfung.
 *
 * <p>Die Feldfolge dreht die des Notenformulars um: zuerst das Datum, denn das
 * ist beim Eintragen einer angekündigten Prüfung meist das Einzige, was schon
 * feststeht. Die Bezeichnung ist freiwillig und sagt das auch.</p>
 */
public final class PlannedExamDialog extends FormDialog<PlannedExamDialog.Result> {

    private final DatePicker datePicker = new DatePicker();
    private final TextField labelField = new TextField();
    private final TextField weightField = new TextField();

    private PlannedExamDialog(Window owner, String title, String confirmLabel, String moduleName) {
        super(owner, title);

        datePicker.setConverter(new GradeDialog.LocalDateConverter());
        labelField.setPromptText(I18n.t("dialog.plannedExam.label.prompt"));
        labelField.setPrefColumnCount(22);
        weightField.setPromptText("1");

        HBox row = new HBox(
                field(I18n.t("field.gradeDate"), datePicker, I18n.t("dialog.plannedExam.date.hint")),
                field(I18n.t("field.gradeWeight"), weightField, I18n.t("dialog.grade.weight.hint")));
        row.getStyleClass().add("dialog-form__row");

        Label context = new Label(I18n.t("dialog.grade.module", moduleName));
        context.getStyleClass().add("label-caption");

        Label note = new Label(I18n.t("dialog.plannedExam.note"));
        note.getStyleClass().add("label-muted");
        note.setMaxWidth(430);

        VBox form = new VBox();
        form.getStyleClass().add("dialog-form");
        form.getChildren().addAll(
                context,
                row,
                field(I18n.t("field.plannedExamLabel"), labelField, null),
                note);

        finish(form, confirmLabel, 480, this::readInput);
        Platform.runLater(datePicker::requestFocus);
    }

    /**
     * Liest und prüft die Eingaben.
     *
     * @return das geprüfte Ergebnis oder {@code null}, wenn eine Eingabe ungültig ist
     */
    private Result readInput() {
        clearErrors();

        LocalDate date = check(datePicker, () -> GradeValidator.requireValidDate(datePicker.getValue()));
        BigDecimal weight = check(weightField,
                () -> GradeValidator.requireValidWeight(GradeValidator.parseDecimal(weightField.getText())));
        String label = check(labelField,
                () -> GradeValidator.requireValidOptionalText(labelField.getText()));

        if (date == null || weight == null || label == null) {
            focusFirstError();
            return null;
        }
        return new Result(label, weight, date);
    }

    /** Dialog zum Eintragen einer geplanten Prüfung. */
    public static Optional<Result> forNewExam(Window owner, String moduleName) {
        PlannedExamDialog form = new PlannedExamDialog(owner,
                I18n.t("dialog.plannedExam.create.title"),
                I18n.t("dialog.plannedExam.create.confirm"), moduleName);
        form.weightField.setText("1");
        return form.showAndWait();
    }

    /** Dialog zum Bearbeiten einer geplanten Prüfung. */
    public static Optional<Result> forExistingExam(Window owner, String moduleName,
                                                   PlannedExam exam) {
        PlannedExamDialog form = new PlannedExamDialog(owner,
                I18n.t("dialog.plannedExam.edit.title"), I18n.t("action.save"), moduleName);
        form.datePicker.setValue(exam.getDate());
        form.labelField.setText(exam.getLabel());
        form.weightField.setText(Formats.weight(exam.getWeight()));
        return form.showAndWait();
    }

    /** Die geprüften Eingaben des Formulars. Die Bezeichnung darf leer sein. */
    public record Result(String label, BigDecimal weight, LocalDate date) {
    }
}
