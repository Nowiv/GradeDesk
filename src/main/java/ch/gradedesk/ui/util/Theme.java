package ch.gradedesk.ui.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.DialogPane;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * Schriften, Stylesheets und Erscheinungsbild.
 *
 * <p>Die Schrift wird mitgeliefert und beim Start geladen. Apples SF Pro darf
 * ausserhalb von Apple-Plattformen nicht weitergegeben werden, deshalb steht
 * hier Inter: eine freie Schrift (SIL Open Font License), die in Proportionen
 * und Duktus sehr nahe an SF Pro liegt. Die Systemschrift des jeweiligen
 * Betriebssystems bleibt als Rückfall eingetragen, falls das Laden scheitert.</p>
 *
 * <p>Das Erscheinungsbild ist eine Stilklasse an der Wurzel der Szene. Weil in
 * {@code theme.css} beide Farbsätze stehen, genügt zum Umschalten ein
 * Klassentausch – nichts muss neu aufgebaut werden.</p>
 */
public final class Theme {

    private static final String THEME_CSS = "/ch/gradedesk/css/theme.css";
    private static final String APP_CSS = "/ch/gradedesk/css/app.css";
    private static final String FONT_DIR = "/ch/gradedesk/font/";

    private static final List<String> FONT_FILES = List.of(
            "Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf",
            "InterDisplay-SemiBold.ttf", "InterDisplay-Bold.ttf");

    private static final ObjectProperty<Appearance> APPEARANCE =
            new SimpleObjectProperty<>(Theme.class, "appearance", Appearance.LIGHT);

    private static boolean fontsLoaded;

    private Theme() {
    }

    /**
     * Lädt die mitgelieferten Schriftschnitte. Mehrfache Aufrufe sind harmlos.
     * Schlägt das Laden fehl, greift die Rückfallkette aus {@code theme.css}.
     */
    public static void loadFonts() {
        if (fontsLoaded) {
            return;
        }
        fontsLoaded = true;
        for (String file : FONT_FILES) {
            try (InputStream stream = Theme.class.getResourceAsStream(FONT_DIR + file)) {
                if (stream != null) {
                    Font.loadFont(stream, 12);
                }
            } catch (Exception e) {
                // Eine fehlende Schrift ist kein Grund, die Anwendung nicht zu starten.
            }
        }
    }

    /** Das eingestellte Erscheinungsbild. */
    public static ReadOnlyObjectProperty<Appearance> appearanceProperty() {
        return APPEARANCE;
    }

    public static Appearance getAppearance() {
        return APPEARANCE.get();
    }

    /** Wechselt zwischen hell und dunkel. */
    public static void setAppearance(Appearance appearance) {
        if (appearance != null && appearance != APPEARANCE.get()) {
            APPEARANCE.set(appearance);
        }
    }

    /** Die Stylesheets als Ressourcen-URLs. */
    public static List<String> stylesheets() {
        return List.of(resource(THEME_CSS), resource(APP_CSS));
    }

    private static String resource(String path) {
        return Objects.requireNonNull(Theme.class.getResource(path),
                "Missing stylesheet: " + path).toExternalForm();
    }

    /**
     * Hängt die Stylesheets an eine Szene und hält ihre Wurzel auf dem
     * eingestellten Erscheinungsbild.
     */
    public static void apply(Scene scene) {
        loadFonts();
        scene.getStylesheets().addAll(stylesheets());
        bindAppearance(scene.getRoot());
    }

    /** Hängt die Stylesheets an einen Dialog und folgt dem Erscheinungsbild. */
    public static void apply(DialogPane dialogPane) {
        loadFonts();
        dialogPane.getStylesheets().addAll(stylesheets());
        bindAppearance(dialogPane);
    }

    /** Hängt die Stylesheets an einen eigenständigen Knoten. */
    public static void apply(Parent parent) {
        loadFonts();
        parent.getStylesheets().addAll(stylesheets());
        bindAppearance(parent);
    }

    /** Setzt die Erscheinungsklasse und hält sie bei späteren Wechseln nach. */
    private static void bindAppearance(Parent root) {
        updateAppearanceClass(root, APPEARANCE.get());
        APPEARANCE.addListener((observable, previous, current) ->
                updateAppearanceClass(root, current));
    }

    private static void updateAppearanceClass(Parent root, Appearance appearance) {
        for (Appearance candidate : Appearance.values()) {
            root.getStyleClass().remove(candidate.styleClass());
        }
        root.getStyleClass().add(appearance.styleClass());
    }

    /** Primäraktion: gefüllte Kapsel in der Akzentfarbe. */
    public static <T extends ButtonBase> T primary(T button) {
        button.getStyleClass().addAll("btn", "btn-filled");
        return button;
    }

    /** Sekundäraktion: getönte Kapsel. */
    public static <T extends ButtonBase> T secondary(T button) {
        button.getStyleClass().addAll("btn", "btn-tinted");
        return button;
    }

    /** Zurückhaltende Textaktion. */
    public static <T extends ButtonBase> T ghost(T button) {
        button.getStyleClass().addAll("btn", "btn-plain");
        return button;
    }

    /** Destruktive Aktion. */
    public static <T extends ButtonBase> T danger(T button) {
        button.getStyleClass().addAll("btn", "btn-destructive");
        return button;
    }
}
