package ch.gradedesk.ui.control;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.model.Grade;
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
 * Formular zum Erfassen und Bearbeiten einer Note.
 *
 * <p>Geprüft wird mit demselben {@link GradeValidator}, den auch das Modell
 * verwendet – es gibt keine zweite, abweichende Regel im UI. Die Mechanik der
 * Feldprüfung steht in {@link FormDialog}.</p>
 */
public final class GradeDialog extends FormDialog<GradeDialog.Result> {

    private final TextField labelField = new TextField();
    private final TextField valueField = new TextField();
    private final TextField weightField = new TextField();
    private final DatePicker datePicker = new DatePicker();

    private GradeDialog(Window owner, String title, String confirmLabel, String moduleName) {
        super(owner, title);

        labelField.setPromptText(I18n.t("dialog.grade.label.prompt"));
        labelField.setPrefColumnCount(22);
        valueField.setPromptText("4.5");
        weightField.setPromptText("1");
        datePicker.setConverter(new LocalDateConverter());

        HBox numbers = new HBox(
                field(I18n.t("field.gradeValue"), valueField, I18n.t("dialog.grade.value.hint")),
                field(I18n.t("field.gradeWeight"), weightField, I18n.t("dialog.grade.weight.hint")));
        numbers.getStyleClass().add("dialog-form__row");

        Label context = new Label(I18n.t("dialog.grade.module", moduleName));
        context.getStyleClass().add("label-caption");

        VBox form = new VBox();
        form.getStyleClass().add("dialog-form");
        form.getChildren().addAll(
                context,
                field(I18n.t("field.gradeLabel"), labelField, null),
                numbers,
                field(I18n.t("field.gradeDate"), datePicker, null));

        finish(form, confirmLabel, 480, this::readInput);
        Platform.runLater(labelField::requestFocus);
    }

    /**
     * Liest und prüft die Eingaben, Feld für Feld.
     *
     * <p>Jedes Feld wird einzeln geprüft, damit im Fehlerfall feststeht, welches
     * gemeint ist – ohne den Meldungstext nach Stichworten durchsuchen zu müssen.</p>
     *
     * @return das geprüfte Ergebnis oder {@code null}, wenn eine Eingabe ungültig ist
     */
    private Result readInput() {
        clearErrors();

        String label = check(labelField, () -> GradeValidator.requireValidText(labelField.getText()));
        BigDecimal value = check(valueField,
                () -> GradeValidator.requireValidGrade(GradeValidator.parseDecimal(valueField.getText())));
        BigDecimal weight = check(weightField,
                () -> GradeValidator.requireValidWeight(GradeValidator.parseDecimal(weightField.getText())));
        LocalDate date = check(datePicker, () -> GradeValidator.requireValidDate(datePicker.getValue()));

        if (label == null || value == null || weight == null || date == null) {
            focusFirstError();
            return null;
        }
        return new Result(label, value, weight, date);
    }

    /** Dialog zum Anlegen einer neuen Note. */
    public static Optional<Result> forNewGrade(Window owner, String moduleName) {
        GradeDialog form = new GradeDialog(owner, I18n.t("dialog.grade.create.title"),
                I18n.t("dialog.grade.create.confirm"), moduleName);
        form.weightField.setText("1");
        form.datePicker.setValue(LocalDate.now());
        return form.showAndWait();
    }

    /** Dialog zum Bearbeiten einer bestehenden Note. */
    public static Optional<Result> forExistingGrade(Window owner, String moduleName, Grade grade) {
        GradeDialog form = new GradeDialog(owner, I18n.t("dialog.grade.edit.title"),
                I18n.t("action.save"), moduleName);
        form.labelField.setText(grade.getLabel());
        form.valueField.setText(Formats.gradeInput(grade.getValue()));
        form.weightField.setText(Formats.weight(grade.getWeight()));
        form.datePicker.setValue(grade.getDate());
        return form.showAndWait();
    }

    /**
     * Dialog für die Note zu einer geplanten Prüfung.
     *
     * <p>Bezeichnung, Gewichtung und Datum stehen bereits da; einzutragen bleibt
     * der Notenwert. Der Fokus beginnt deshalb im Notenfeld.</p>
     */
    public static Optional<Result> forPlannedExam(Window owner, String moduleName,
                                                  PlannedExam exam) {
        GradeDialog form = new GradeDialog(owner, I18n.t("dialog.grade.fromPlanned.title"),
                I18n.t("dialog.grade.create.confirm"), moduleName);
        form.labelField.setText(exam.getLabel());
        form.weightField.setText(Formats.weight(exam.getWeight()));
        form.datePicker.setValue(exam.getDate());
        Platform.runLater(form.valueField::requestFocus);
        return form.showAndWait();
    }

    /** Die geprüften Eingaben des Formulars. */
    public record Result(String label, BigDecimal value, BigDecimal weight, LocalDate date) {
    }

    /** Zeigt und liest das Datum im Format der eingestellten Sprache. */
    static final class LocalDateConverter extends javafx.util.StringConverter<LocalDate> {

        @Override
        public String toString(LocalDate date) {
            return date == null ? "" : Formats.date(date);
        }

        @Override
        public LocalDate fromString(String text) {
            if (text == null || text.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(text.trim(), java.time.format.DateTimeFormatter
                        .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                        .withLocale(I18n.getLanguage().locale()));
            } catch (java.time.format.DateTimeParseException e) {
                // Unlesbare Eingabe wird wie «kein Datum» behandelt; die Prüfung meldet das.
                return null;
            }
        }
    }
}
