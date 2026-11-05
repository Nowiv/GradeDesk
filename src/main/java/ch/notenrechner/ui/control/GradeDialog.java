package ch.notenrechner.ui.control;

import ch.notenrechner.model.Grade;
import ch.notenrechner.service.GradeValidator;
import ch.notenrechner.service.ValidationException;
import ch.notenrechner.ui.util.Formats;
import ch.notenrechner.ui.util.Theme;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
import java.util.Optional;

/**
 * Formular zum Erfassen und Bearbeiten einer Note.
 *
 * <p>Geprüft wird mit demselben {@link GradeValidator}, den auch das Modell
 * verwendet – es gibt keine zweite, abweichende Regel im UI. Eine ungültige
 * Eingabe schliesst den Dialog nicht, sondern markiert das betroffene Feld und
 * erklärt darunter, was zu tun ist.</p>
 */
public final class GradeDialog {

    private final Dialog<Result> dialog = new Dialog<>();
    private final TextField labelField = new TextField();
    private final TextField valueField = new TextField();
    private final TextField weightField = new TextField();
    private final DatePicker datePicker = new DatePicker();
    private final Label errorLabel = new Label();

    private GradeDialog(Window owner, String title, String confirmLabel, String moduleName) {
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Notenrechner");
        dialog.setHeaderText(title);

        labelField.setPromptText("z.B. Prüfung 1");
        labelField.setPrefColumnCount(22);
        valueField.setPromptText("4.5");
        weightField.setPromptText("1");

        errorLabel.getStyleClass().add("field-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        HBox numbers = new HBox(
                field("Note", valueField, "Zwischen 1.0 und 6.0"),
                field("Gewichtung", weightField, "Standard 1, grösser als 0"));
        numbers.getStyleClass().add("dialog-form__row");

        VBox form = new VBox();
        form.getStyleClass().add("dialog-form");
        Label context = new Label("Modul: " + moduleName);
        context.getStyleClass().add("label-caption");
        form.getChildren().addAll(
                context,
                field("Bezeichnung", labelField, null),
                numbers,
                field("Datum", datePicker, null),
                errorLabel);

        ButtonType confirm = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        DialogPane pane = dialog.getDialogPane();
        Theme.apply(pane);
        pane.setContent(form);
        pane.setMinWidth(460);
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

    private VBox field(String labelText, javafx.scene.control.Control control, String hint) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        label.setLabelFor(control);

        VBox box = new VBox(label, control);
        box.getStyleClass().add("dialog-form__field");
        if (hint != null) {
            Label hintLabel = new Label(hint);
            hintLabel.getStyleClass().add("field-hint");
            box.getChildren().add(hintLabel);
        }
        HBox.setHgrow(box, Priority.ALWAYS);
        control.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /**
     * Liest und prüft die Eingaben.
     *
     * @return das geprüfte Ergebnis oder {@code null}, wenn eine Eingabe ungültig ist
     *         (die Meldung steht dann im Formular)
     */
    private Result readInput() {
        clearErrors();
        try {
            String label = GradeValidator.requireValidText(labelField.getText(), "Bezeichnung");
            BigDecimal value = GradeValidator.requireValidGrade(
                    GradeValidator.parseDecimal(valueField.getText(), "Note"));
            BigDecimal weight = GradeValidator.requireValidWeight(
                    GradeValidator.parseDecimal(weightField.getText(), "Gewichtung"));
            LocalDate date = GradeValidator.requireValidDate(datePicker.getValue());
            return new Result(label, value, weight, date);
        } catch (ValidationException e) {
            showError(e.getMessage());
            return null;
        }
    }

    private void clearErrors() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        labelField.getStyleClass().remove("text-field--invalid");
        valueField.getStyleClass().remove("text-field--invalid");
        weightField.getStyleClass().remove("text-field--invalid");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        // Das Feld markieren, auf das sich die Meldung bezieht.
        TextField culprit = null;
        if (message.contains("Bezeichnung")) {
            culprit = labelField;
        } else if (message.contains("Note")) {
            culprit = valueField;
        } else if (message.contains("Gewichtung")) {
            culprit = weightField;
        }
        if (culprit != null) {
            culprit.getStyleClass().add("text-field--invalid");
            culprit.requestFocus();
        }
    }

    /** Dialog zum Anlegen einer neuen Note. */
    public static Optional<Result> forNewGrade(Window owner, String moduleName) {
        GradeDialog form = new GradeDialog(owner, "Note erfassen", "Erfassen", moduleName);
        form.weightField.setText("1");
        form.datePicker.setValue(LocalDate.now());
        return form.dialog.showAndWait();
    }

    /** Dialog zum Bearbeiten einer bestehenden Note. */
    public static Optional<Result> forExistingGrade(Window owner, String moduleName, Grade grade) {
        GradeDialog form = new GradeDialog(owner, "Note bearbeiten", "Speichern", moduleName);
        form.labelField.setText(grade.getLabel());
        form.valueField.setText(Formats.gradeInput(grade.getValue()));
        form.weightField.setText(Formats.weight(grade.getWeight()));
        form.datePicker.setValue(grade.getDate());
        return form.dialog.showAndWait();
    }

    /** Die geprüften Eingaben des Formulars. */
    public record Result(String label, BigDecimal value, BigDecimal weight, LocalDate date) {
    }
}
