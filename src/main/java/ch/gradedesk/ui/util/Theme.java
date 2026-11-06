package ch.gradedesk.ui.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
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
 * <p>Das Erscheinungsbild wird über die Stylesheet-Liste umgeschaltet:
 * {@code theme.css} enthält die helle Palette, im dunklen Erscheinungsbild
 * kommt {@code theme-dark.css} dazu und überschreibt sie. Beide Dateien
 * definieren ihre Variablen auf {@code .root}.</p>
 *
 * <p>Eine blosse Stilklasse an der Wurzel würde nicht genügen: JavaFX gibt
 * einem Popup-Fenster (Tooltip, Kontextmenü, Kalender des DatePicker) zwar die
 * Stylesheets der Szene mit, aber nicht die Stilklassen ihrer Wurzel. Eine
 * Palette unter {@code .appearance-dark} fiele dort auf die helle Variante
 * zurück. Die Klasse wird trotzdem gesetzt, damit farblose Regeln sich am
 * Erscheinungsbild orientieren können.</p>
 */
public final class Theme {

    private static final String THEME_CSS = "/ch/gradedesk/css/theme.css";
    private static final String THEME_DARK_CSS = "/ch/gradedesk/css/theme-dark.css";
    private static final String APP_CSS = "/ch/gradedesk/css/app.css";
    private static final String FONT_DIR = "/ch/gradedesk/font/";

    private static final List<String> FONT_FILES = List.of(
            "Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf",
            "InterDisplay-SemiBold.ttf", "InterDisplay-Bold.ttf");

    private static final ObjectProperty<Appearance> APPEARANCE =
            new SimpleObjectProperty<>(Theme.class, "appearance", Appearance.LIGHT);

    /**
     * Alles, was dem Erscheinungsbild folgt.
     *
     * <p>Die Verweise sind schwach: eine geschlossene Dialogszene darf nicht
     * deshalb im Speicher bleiben, weil das Erscheinungsbild sie kennt.</p>
     */
    private static final List<Target> TARGETS = new ArrayList<>();

    private static boolean fontsLoaded;

    static {
        APPEARANCE.addListener((observable, previous, current) -> refreshTargets(current));
    }

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

    /** Die Stylesheets des eingestellten Erscheinungsbilds als Ressourcen-URLs. */
    public static List<String> stylesheets() {
        return stylesheets(APPEARANCE.get());
    }

    private static List<String> stylesheets(Appearance appearance) {
        if (appearance == Appearance.DARK) {
            return List.of(resource(THEME_CSS), resource(THEME_DARK_CSS), resource(APP_CSS));
        }
        return List.of(resource(THEME_CSS), resource(APP_CSS));
    }

    /** Jedes Stylesheet, das diese Klasse anhängen kann – zum Entfernen beim Wechsel. */
    private static List<String> allStylesheets() {
        return List.of(resource(THEME_CSS), resource(THEME_DARK_CSS), resource(APP_CSS));
    }

    private static String resource(String path) {
        return Objects.requireNonNull(Theme.class.getResource(path),
                "Missing stylesheet: " + path).toExternalForm();
    }

    /**
     * Hängt die Stylesheets an eine Szene und hält sie auf dem eingestellten
     * Erscheinungsbild.
     *
     * <p>Die Szene ist der richtige Ort: Popup-Fenster übernehmen die
     * Stylesheets der Szene, zu der ihr Auslöser gehört.</p>
     */
    public static void apply(Scene scene) {
        loadFonts();
        register(scene.getStylesheets(), scene.getRoot());
    }

    /**
     * Hängt die Stylesheets an einen eigenständigen Knoten – etwa an den
     * {@code DialogPane} eines Dialogs, der eine eigene Szene bekommt.
     */
    public static void apply(Parent parent) {
        loadFonts();
        register(parent.getStylesheets(), parent);
    }

    private static void register(ObservableList<String> stylesheets, Parent root) {
        applyTo(stylesheets, root, APPEARANCE.get());
        TARGETS.add(new Target(new WeakReference<>(stylesheets), new WeakReference<>(root)));
    }

    private static void refreshTargets(Appearance appearance) {
        Iterator<Target> targets = TARGETS.iterator();
        while (targets.hasNext()) {
            Target target = targets.next();
            ObservableList<String> stylesheets = target.stylesheets().get();
            Parent root = target.root().get();
            if (stylesheets == null && root == null) {
                targets.remove();
                continue;
            }
            applyTo(stylesheets, root, appearance);
        }
    }

    private static void applyTo(ObservableList<String> stylesheets, Parent root,
                                Appearance appearance) {
        if (stylesheets != null) {
            stylesheets.removeAll(allStylesheets());
            stylesheets.addAll(stylesheets(appearance));
        }
        if (root != null) {
            updateAppearanceClass(root, appearance);
        }
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

    /** Ein Ziel, das dem Erscheinungsbild folgt, ohne es am Leben zu halten. */
    private record Target(WeakReference<ObservableList<String>> stylesheets,
                          WeakReference<Parent> root) {
    }
}
