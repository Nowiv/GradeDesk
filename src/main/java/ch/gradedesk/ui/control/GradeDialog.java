package ch.gradedesk.ui.control;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.model.Grade;
import ch.gradedesk.service.GradeValidator;
import ch.gradedesk.service.ValidationException;
import ch.gradedesk.ui.util.Formats;
import ch.gradedesk.ui.util.Theme;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Formular zum Erfassen und Bearbeiten einer Note.
 *
 * <p>Geprüft wird mit demselben {@link GradeValidator}, den auch das Modell
 * verwendet – es gibt keine zweite, abweichende Regel im UI. Eine ungültige
 * Eingabe schliesst den Dialog nicht, sondern markiert genau das betroffene
 * Feld und schreibt die Erklärung darunter.</p>
 */
public final class GradeDialog {

    private final Dialog<Result> dialog = new Dialog<>();
    private final TextField labelField = new TextField();
    private final TextField valueField = new TextField();
    private final TextField weightField = new TextField();
    private final DatePicker datePicker = new DatePicker();

    /** Fehlerzeile je Eingabefeld, damit die Meldung dort steht, wo sie hingehört. */
    private final Map<Control, Label> errorLabels = new LinkedHashMap<>();

    private GradeDialog(Window owner, String title, String confirmLabel, String moduleName) {
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(I18n.t("app.name"));
        dialog.setHeaderText(title);

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

        ButtonType confirm = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(I18n.t("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        DialogPane pane = dialog.getDialogPane();
        Theme.apply(pane);
        pane.setContent(form);
        pane.setMinWidth(480);
        pane.getButtonTypes().setAll(cancel, confirm);
        Theme.primary((Button) pane.lookupButton(confirm));
        Theme.secondary((Button) pane.lookupButton(cancel));

        Button confirmButton = (Button) pane.lookupButton(confirm);
        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (readInput() == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(button -> button == confirm ? readInput() : null);
        Platform.runLater(labelField::requestFocus);
    }

    private VBox field(String labelText, Control control, String hint) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        label.setLabelFor(control);

        Label error = new Label();
        error.getStyleClass().add("field-error");
        error.setVisible(false);
        error.setManaged(false);
        error.setWrapText(true);
        errorLabels.put(control, error);

        VBox box = new VBox(label, control);
        box.getStyleClass().add("dialog-form__field");
        if (hint != null) {
            Label hintLabel = new Label(hint);
            hintLabel.getStyleClass().add("field-hint");
            box.getChildren().add(hintLabel);
        }
        box.getChildren().add(error);

        HBox.setHgrow(box, Priority.ALWAYS);
        control.setMaxWidth(Double.MAX_VALUE);
        return box;
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
            errorLabels.entrySet().stream()
                    .filter(entry -> entry.getValue().isVisible())
                    .findFirst()
                    .ifPresent(entry -> entry.getKey().requestFocus());
            return null;
        }
        return new Result(label, value, weight, date);
    }

    /** Führt eine Prüfung aus und schreibt eine allfällige Meldung unter das Feld. */
    private <T> T check(Control control, Supplier<T> validation) {
        try {
            return validation.get();
        } catch (ValidationException e) {
            Label error = errorLabels.get(control);
            error.setText(I18n.t(e.getMessageKey(), e.getArguments()));
            error.setVisible(true);
            error.setManaged(true);
            if (!control.getStyleClass().contains("text-field--invalid")) {
                control.getStyleClass().add("text-field--invalid");
            }
            return null;
        }
    }

    private void clearErrors() {
        errorLabels.forEach((control, error) -> {
            error.setVisible(false);
            error.setManaged(false);
            control.getStyleClass().remove("text-field--invalid");
        });
    }

    /** Dialog zum Anlegen einer neuen Note. */
    public static Optional<Result> forNewGrade(Window owner, String moduleName) {
        GradeDialog form = new GradeDialog(owner, I18n.t("dialog.grade.create.title"),
                I18n.t("dialog.grade.create.confirm"), moduleName);
        form.weightField.setText("1");
        form.datePicker.setValue(LocalDate.now());
        return form.dialog.showAndWait();
    }

    /** Dialog zum Bearbeiten einer bestehenden Note. */
    public static Optional<Result> forExistingGrade(Window owner, String moduleName, Grade grade) {
        GradeDialog form = new GradeDialog(owner, I18n.t("dialog.grade.edit.title"),
                I18n.t("action.save"), moduleName);
        form.labelField.setText(grade.getLabel());
        form.valueField.setText(Formats.gradeInput(grade.getValue()));
        form.weightField.setText(Formats.weight(grade.getWeight()));
        form.datePicker.setValue(grade.getDate());
        return form.dialog.showAndWait();
    }

    /** Die geprüften Eingaben des Formulars. */
    public record Result(String label, BigDecimal value, BigDecimal weight, LocalDate date) {
    }

    /** Zeigt und liest das Datum im Format der eingestellten Sprache. */
    private static final class LocalDateConverter extends javafx.util.StringConverter<LocalDate> {

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
