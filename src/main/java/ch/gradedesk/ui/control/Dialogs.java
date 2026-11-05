package ch.gradedesk.ui.control;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.service.ValidationException;
import ch.gradedesk.ui.util.Icons;
import ch.gradedesk.ui.util.Theme;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Die wiederkehrenden Dialoge in einheitlicher Gestaltung.
 *
 * <p>Löschen verlangt immer eine Bestätigung, und die Bestätigung benennt, was
 * genau verschwindet – nicht «Sind Sie sicher?», sondern die Folge.</p>
 */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * Bestätigung für eine destruktive Aktion.
     *
     * @param owner        Fenster, über dem der Dialog erscheint
     * @param title        Titelzeile, z.B. «Modul löschen?»
     * @param consequence  was dabei verloren geht, in einem Satz
     * @param confirmLabel Beschriftung der bestätigenden Schaltfläche
     * @return {@code true}, wenn die Aktion ausgeführt werden soll
     */
    public static boolean confirmDelete(Window owner, String title, String consequence,
                                        String confirmLabel) {
        Dialog<ButtonType> dialog = baseDialog(owner, title);
        ButtonType confirm = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(I18n.t("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        DialogPane pane = dialog.getDialogPane();
        pane.setContentText(consequence);
        pane.getButtonTypes().setAll(cancel, confirm);

        Button confirmButton = Theme.danger((Button) pane.lookupButton(confirm));
        Button cancelButton = Theme.secondary((Button) pane.lookupButton(cancel));

        // Abbrechen ist die Standardaktion: ein versehentliches Enter löscht nichts.
        confirmButton.setDefaultButton(false);
        cancelButton.setDefaultButton(true);

        return dialog.showAndWait().filter(confirm::equals).isPresent();
    }

    /** Hinweis auf einen Fehler, den der Benutzer kennen muss. */
    public static void error(Window owner, String title, String message) {
        Dialog<ButtonType> dialog = baseDialog(owner, title);
        DialogPane pane = dialog.getDialogPane();
        pane.setContentText(message);
        pane.setGraphic(Icons.create(Icons.ALERT, 28, "icon--alert"));
        pane.getButtonTypes().setAll(ButtonType.OK);
        Theme.primary((Button) pane.lookupButton(ButtonType.OK));
        dialog.showAndWait();
    }

    /** Sachliche Information, etwa nach einer Wiederherstellung aus dem Backup. */
    public static void info(Window owner, String title, String message) {
        Dialog<ButtonType> dialog = baseDialog(owner, title);
        DialogPane pane = dialog.getDialogPane();
        pane.setContentText(message);
        pane.getButtonTypes().setAll(ButtonType.OK);
        Theme.primary((Button) pane.lookupButton(ButtonType.OK));
        dialog.showAndWait();
    }

    /**
     * Fragt einen einzelnen Text ab, etwa einen Semester- oder Modulnamen.
     *
     * <p>Die Prüfung läuft über denselben Validator wie das Modell. Eine
     * ungültige Eingabe schliesst den Dialog nicht, sondern erklärt den Fehler
     * direkt unter dem Feld.</p>
     *
     * @param validator wirft {@link ValidationException} bei ungültiger Eingabe
     * @return der geprüfte Text oder {@link Optional#empty()} bei Abbruch
     */
    public static Optional<String> promptText(Window owner, String title, String explanation,
                                              String fieldLabel, String initialValue,
                                              String confirmLabel, TextValidator validator) {
        Dialog<String> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(I18n.t("app.name"));
        dialog.setHeaderText(title);

        TextField field = new TextField(initialValue == null ? "" : initialValue);
        field.setPromptText(fieldLabel);
        field.setPrefColumnCount(24);

        Label error = new Label();
        error.getStyleClass().add("field-error");
        error.setVisible(false);
        error.setManaged(false);

        Label label = new Label(fieldLabel);
        label.getStyleClass().add("field-label");
        label.setLabelFor(field);

        VBox fieldBox = new VBox(label, field, error);
        fieldBox.getStyleClass().add("dialog-form__field");

        VBox form = new VBox();
        form.getStyleClass().add("dialog-form");
        if (explanation != null && !explanation.isBlank()) {
            Label hint = new Label(explanation);
            hint.getStyleClass().add("label-muted");
            hint.setMaxWidth(380);
            form.getChildren().add(hint);
        }
        form.getChildren().add(fieldBox);

        ButtonType confirm = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(I18n.t("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        DialogPane pane = dialog.getDialogPane();
        Theme.apply(pane);
        pane.setContent(form);
        pane.setMinWidth(440);
        pane.getButtonTypes().setAll(cancel, confirm);
        Theme.primary((Button) pane.lookupButton(confirm));
        Theme.secondary((Button) pane.lookupButton(cancel));

        Button confirmButton = (Button) pane.lookupButton(confirm);
        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                validator.validate(field.getText());
                field.getStyleClass().remove("text-field--invalid");
            } catch (ValidationException e) {
                event.consume();
                error.setText(I18n.t(e.getMessageKey(), e.getArguments()));
                error.setVisible(true);
                error.setManaged(true);
                if (!field.getStyleClass().contains("text-field--invalid")) {
                    field.getStyleClass().add("text-field--invalid");
                }
                field.requestFocus();
            }
        });

        dialog.setResultConverter(button -> button == confirm ? field.getText().trim() : null);
        Platform.runLater(() -> {
            field.requestFocus();
            field.selectAll();
        });

        return dialog.showAndWait();
    }

    private static Dialog<ButtonType> baseDialog(Window owner, String title) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle(I18n.t("app.name"));
        dialog.setHeaderText(title);
        Theme.apply(dialog.getDialogPane());
        dialog.getDialogPane().setMinWidth(440);
        return dialog;
    }

    /** Wrapper, damit der Aufrufer dieselbe Prüfung wie das Modell verwenden kann. */
    @FunctionalInterface
    public interface TextValidator {
        void validate(String text);
    }
}
