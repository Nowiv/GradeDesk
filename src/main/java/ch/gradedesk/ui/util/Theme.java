package ch.gradedesk.ui.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.DialogPane;

import java.util.List;
import java.util.Objects;

/**
 * Lädt die Stylesheets und vergibt die Standard-Stilklassen für Schaltflächen.
 *
 * <p>Die Reihenfolge ist wichtig: {@code theme.css} definiert die Farbnamen,
 * {@code app.css} verwendet sie.</p>
 */
public final class Theme {

    private static final String THEME_CSS = "/ch/gradedesk/css/theme.css";
    private static final String APP_CSS = "/ch/gradedesk/css/app.css";

    private Theme() {
    }

    /** Die Stylesheets als Ressourcen-URLs. */
    public static List<String> stylesheets() {
        return List.of(resource(THEME_CSS), resource(APP_CSS));
    }

    private static String resource(String path) {
        return Objects.requireNonNull(Theme.class.getResource(path),
                "Stylesheet fehlt im Paket: " + path).toExternalForm();
    }

    /** Hängt die Stylesheets an eine Szene. */
    public static void apply(Scene scene) {
        scene.getStylesheets().addAll(stylesheets());
    }

    /** Hängt die Stylesheets an einen Dialog. */
    public static void apply(DialogPane dialogPane) {
        dialogPane.getStylesheets().addAll(stylesheets());
    }

    /** Hängt die Stylesheets an einen eigenständigen Knoten (z.B. einen Dialoginhalt). */
    public static void apply(Parent parent) {
        parent.getStylesheets().addAll(stylesheets());
    }

    /** Macht eine Schaltfläche zur Primäraktion. */
    public static <T extends ButtonBase> T primary(T button) {
        button.getStyleClass().addAll("btn", "btn-primary");
        return button;
    }

    /** Macht eine Schaltfläche zur Sekundäraktion. */
    public static <T extends ButtonBase> T secondary(T button) {
        button.getStyleClass().addAll("btn", "btn-secondary");
        return button;
    }

    /** Macht eine Schaltfläche zur zurückhaltenden Textaktion. */
    public static <T extends ButtonBase> T ghost(T button) {
        button.getStyleClass().addAll("btn", "btn-ghost");
        return button;
    }

    /** Macht eine Schaltfläche zur destruktiven Aktion. */
    public static <T extends ButtonBase> T danger(T button) {
        button.getStyleClass().addAll("btn", "btn-danger");
        return button;
    }
}
