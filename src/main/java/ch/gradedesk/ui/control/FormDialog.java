package ch.gradedesk.ui.control;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.service.ValidationException;
import ch.gradedesk.ui.util.Theme;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Gemeinsames Gerüst der Formulardialoge.
 *
 * <p>Beide Formulare – Note und geplante Prüfung – halten sich an dieselbe
 * Regel: geprüft wird mit demselben Validator wie im Modell, und eine ungültige
 * Eingabe schliesst den Dialog nicht, sondern markiert genau das betroffene Feld
 * und schreibt die Erklärung darunter. Diese Mechanik steht hier einmal.</p>
 *
 * @param <T> das geprüfte Ergebnis des Formulars
 */
abstract class FormDialog<T> {

    protected final Dialog<T> dialog = new Dialog<>();

    /** Fehlerzeile je Eingabefeld, damit die Meldung dort steht, wo sie hingehört. */
    private final Map<Control, Label> errorLabels = new LinkedHashMap<>();

    protected FormDialog(Window owner, String title) {
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(I18n.t("app.name"));
        dialog.setHeaderText(title);
    }

    /**
     * Baut ein beschriftetes Feld mit Platz für eine Fehlermeldung darunter.
     *
     * @param hint erklärender Satz unter dem Feld, oder {@code null}
     */
    protected VBox field(String labelText, Control control, String hint) {
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
     * Setzt Inhalt und Schaltflächen und verdrahtet die Prüfung.
     *
     * <p>Der Leser liefert {@code null}, wenn eine Eingabe ungültig ist; der
     * Dialog bleibt dann offen.</p>
     */
    protected void finish(VBox form, String confirmLabel, double minWidth, Supplier<T> reader) {
        ButtonType confirm = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(I18n.t("action.cancel"),
                ButtonBar.ButtonData.CANCEL_CLOSE);

        DialogPane pane = dialog.getDialogPane();
        Theme.apply(pane);
        pane.setContent(form);
        pane.setMinWidth(minWidth);
        pane.getButtonTypes().setAll(cancel, confirm);
        Theme.primary((Button) pane.lookupButton(confirm));
        Theme.secondary((Button) pane.lookupButton(cancel));

        Button confirmButton = (Button) pane.lookupButton(confirm);
        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (reader.get() == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(button -> button == confirm ? reader.get() : null);
    }

    /** Führt eine Prüfung aus und schreibt eine allfällige Meldung unter das Feld. */
    protected <V> V check(Control control, Supplier<V> validation) {
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

    protected void clearErrors() {
        errorLabels.forEach((control, error) -> {
            error.setVisible(false);
            error.setManaged(false);
            control.getStyleClass().remove("text-field--invalid");
        });
    }

    /** Setzt den Fokus auf das erste Feld, das eine Meldung trägt. */
    protected void focusFirstError() {
        errorLabels.entrySet().stream()
                .filter(entry -> entry.getValue().isVisible())
                .findFirst()
                .ifPresent(entry -> entry.getKey().requestFocus());
    }

    protected Optional<T> showAndWait() {
        return dialog.showAndWait();
    }
}
