package ch.notenrechner.ui.control;

import ch.notenrechner.service.ValidationException;
import ch.notenrechner.ui.util.Icons;
import ch.notenrechner.ui.util.Theme;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Die wiederkehrenden Dialoge der Anwendung in einheitlicher Gestaltung.
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
        ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        DialogPane pane = dialog.getDialogPane();
        pane.setContentText(consequence);
        pane.getButtonTypes().setAll(cancel, confirm);

        Theme.danger((Button) pane.lookupButton(confirm));
        Theme.secondary((Button) pane.lookupButton(cancel));

        Button confirmButton = (Button) pane.lookupButton(confirm);
        confirmButton.setDefaultButton(false);
        Button cancelButton = (Button) pane.lookupButton(cancel);
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
        dialog.setTitle("Notenrechner");
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

        VBox fieldBox = new VBox(label, field, error);
        fieldBox.getStyleClass().add("dialog-form__field");

        VBox form = new VBox();
        form.getStyleClass().add("dialog-form");
        if (explanation != null && !explanation.isBlank()) {
            Label hint = new Label(explanation);
            hint.getStyleClass().add("label-muted");
            hint.setMaxWidth(360);
            form.getChildren().add(hint);
        }
        form.getChildren().add(fieldBox);

        ButtonType confirm = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        DialogPane pane = dialog.getDialogPane();
        Theme.apply(pane);
        pane.setContent(form);
        pane.getButtonTypes().setAll(cancel, confirm);
        Theme.primary((Button) pane.lookupButton(confirm));
        Theme.secondary((Button) pane.lookupButton(cancel));

        Button confirmButton = (Button) pane.lookupButton(confirm);
        confirmButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                validator.validate(field.getText());
                field.getStyleClass().remove("text-field--invalid");
            } catch (ValidationException e) {
                event.consume();
                error.setText(e.getMessage());
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
        dialog.setTitle("Notenrechner");
        dialog.setHeaderText(title);
        Theme.apply(dialog.getDialogPane());
        dialog.getDialogPane().setMinWidth(420);
        return dialog;
    }

    /** Wrapper, damit der Aufrufer dieselbe Prüfung wie das Modell verwenden kann. */
    @FunctionalInterface
    public interface TextValidator {
        void validate(String text) throws ValidationException;
    }

    /** Eine Zeile mit Abstand für Dialog-Inhalte. */
    static HBox row(javafx.scene.Node... children) {
        HBox box = new HBox(children);
        box.getStyleClass().add("dialog-form__row");
        return box;
    }
}
