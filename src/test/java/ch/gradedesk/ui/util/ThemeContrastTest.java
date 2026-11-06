package ch.gradedesk.ui.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Prüft die beiden Farbpaletten, ohne JavaFX zu starten.
 *
 * <p>Drei Zusagen werden hier festgehalten:</p>
 * <ol>
 *   <li>Farbwerte stehen ausschliesslich in den Theme-Dateien.</li>
 *   <li>Beide Paletten führen genau dieselben Variablen; eine Farbe kann nicht
 *       nur in einem Erscheinungsbild existieren.</li>
 *   <li>Jede Text- und Elementkombination erfüllt WCAG AA: 4.5:1 für Text,
 *       3:1 für grosse Schrift und Bedienelemente.</li>
 * </ol>
 *
 * <p>Die Prüfung läuft auf beiden Paletten. Eine Änderung, die das dunkle
 * Erscheinungsbild verbessert, kann das helle damit nicht verschlechtern.</p>
 */
class ThemeContrastTest {

    private static final String LIGHT_CSS = "/ch/gradedesk/css/theme.css";
    private static final String DARK_CSS = "/ch/gradedesk/css/theme-dark.css";
    private static final String APP_CSS = "/ch/gradedesk/css/app.css";

    /** Mindestkontrast für Text. */
    private static final double TEXT = 4.5;

    /** Mindestkontrast für grosse Schrift und Bedienelemente. */
    private static final double ELEMENT = 3.0;

    private static final Pattern VARIABLE =
            Pattern.compile("(-ui-[a-z0-9-]+)\\s*:\\s*([^;]+);");

    private static final Pattern REFERENCE = Pattern.compile("-ui-[a-z0-9-]+");

    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private static final Pattern LITERAL_COLOUR = Pattern.compile(
            "#[0-9a-fA-F]{3,8}\\b|\\brgba?\\s*\\(|\\b(?:white|black|red|green|blue|gray|grey"
                    + "|yellow|orange|silver|transparent-white)\\b");

    /** Eine Kombination, die lesbar bleiben muss. */
    private record Combination(String what, String foreground, String background, double minimum) {
    }

    /**
     * Die Kombinationen, wie sie in app.css tatsächlich vorkommen.
     *
     * <p>Wer eine Regel in app.css ergänzt, die Text auf einen neuen
     * Hintergrund stellt, trägt sie hier nach.</p>
     */
    private static final List<Combination> COMBINATIONS = List.of(
            new Combination("Fliesstext auf Karte", "-ui-text", "-ui-bg-surface", TEXT),
            new Combination("Fliesstext auf Arbeitsflaeche", "-ui-text", "-ui-bg-base", TEXT),
            new Combination("Navigationstext auf Seitenleiste", "-ui-text", "-ui-bg-sidebar", TEXT),
            new Combination("Sekundaertext auf Karte", "-ui-text-secondary", "-ui-bg-surface", TEXT),
            new Combination("Sekundaertext auf Arbeitsflaeche", "-ui-text-secondary", "-ui-bg-base", TEXT),
            new Combination("Speicherzustand auf Seitenleiste", "-ui-text-secondary", "-ui-bg-sidebar", TEXT),
            new Combination("Leiser Text auf Seitenleiste", "-ui-text-tertiary", "-ui-bg-sidebar", TEXT),
            new Combination("Platzhaltertext im Eingabefeld", "-ui-text-tertiary", "-ui-bg-input", TEXT),
            new Combination("Nachbarmonat im Kalender", "-ui-text-quaternary", "-ui-bg-overlay", TEXT),
            new Combination("Note ohne Wert, gross", "-ui-text-quaternary", "-ui-bg-surface", ELEMENT),
            new Combination("Beschriftung auf Primaerschaltflaeche", "-ui-text-on-accent", "-ui-accent", TEXT),
            new Combination("Primaerschaltflaeche ueberfahren", "-ui-text-on-accent", "-ui-accent-bright", TEXT),
            new Combination("Primaerschaltflaeche gedrueckt", "-ui-text-on-accent", "-ui-accent-pressed", TEXT),
            new Combination("Markierter Text im Eingabefeld", "-ui-text", "-ui-accent-wash", TEXT),
            new Combination("Text im fokussierten Eingabefeld", "-ui-text", "-ui-bg-input-focus", TEXT),
            new Combination("Text auf Sekundaerschaltflaeche", "-ui-text", "-ui-bg-raised", TEXT),
            new Combination("Text auf Loeschen-Schaltflaeche", "-ui-fail", "-ui-bg-raised", TEXT),
            new Combination("Abzeichen ungenuegend", "-ui-fail", "-ui-fail-wash", TEXT),
            new Combination("Abzeichen genuegend", "-ui-pass", "-ui-pass-wash", TEXT),
            new Combination("Text in der Hinweisleiste", "-ui-text", "-ui-warn-wash", TEXT),
            new Combination("Achsenbeschriftung im Diagramm", "-ui-text-secondary", "-ui-bg-surface", TEXT),
            new Combination("Text in Tooltip, Menue und Flyout", "-ui-text", "-ui-bg-overlay", TEXT),
            new Combination("Text auf ueberfahrener Zeile", "-ui-text", "-ui-row-hover", TEXT),
            new Combination("Text auf ausgewaehlter Zeile", "-ui-text", "-ui-selection", TEXT),
            new Combination("Text auf ueberfahrener Schaltflaeche", "-ui-text", "-ui-control-hover", TEXT),
            new Combination("Fokusring auf Arbeitsflaeche", "-ui-focus", "-ui-bg-base", ELEMENT),
            new Combination("Fokusring auf Seitenleiste", "-ui-focus", "-ui-bg-sidebar", ELEMENT),
            new Combination("Fokusring auf Karte", "-ui-focus", "-ui-bg-surface", ELEMENT),
            new Combination("Akzentmarke auf Karte", "-ui-accent", "-ui-bg-surface", ELEMENT),
            new Combination("Akzentmarke auf Seitenleiste", "-ui-accent", "-ui-bg-sidebar", ELEMENT),
            new Combination("Unterkante Eingabefeld", "-ui-stroke-input-bottom", "-ui-bg-input", ELEMENT),
            new Combination("Auswahlkante gegen Karte", "-ui-selection-stroke", "-ui-bg-surface", ELEMENT),
            new Combination("Bildlaufgriff auf Arbeitsflaeche", "-ui-scroll-thumb", "-ui-bg-base", ELEMENT),
            new Combination("Statuspunkt gespeichert", "-ui-pass", "-ui-bg-sidebar", ELEMENT),
            new Combination("Statuspunkt ausstehend", "-ui-warn", "-ui-bg-sidebar", ELEMENT),
            new Combination("Statuspunkt fehlgeschlagen", "-ui-fail", "-ui-bg-sidebar", ELEMENT),
            new Combination("Saeule 4.0 gegen Karte", "-ui-pass-light", "-ui-bg-surface", ELEMENT),
            new Combination("Saeule 4.5 gegen Karte", "-ui-pass-mid", "-ui-bg-surface", ELEMENT),
            new Combination("Saeule 5.0 gegen Karte", "-ui-pass-base", "-ui-bg-surface", ELEMENT),
            new Combination("Saeule 5.5 gegen Karte", "-ui-pass-strong", "-ui-bg-surface", ELEMENT),
            new Combination("Saeule 6.0 gegen Karte", "-ui-pass-deep", "-ui-bg-surface", ELEMENT),
            new Combination("Saeule ungenuegend gegen Karte", "-ui-fail", "-ui-bg-surface", ELEMENT),
            new Combination("Notenband 4.0 gegen Schiene", "-ui-pass-light", "-ui-track", ELEMENT),
            new Combination("Notenband 4.5 gegen Schiene", "-ui-pass-mid", "-ui-track", ELEMENT),
            new Combination("Notenband 5.0 gegen Schiene", "-ui-pass-base", "-ui-track", ELEMENT),
            new Combination("Notenband 5.5 gegen Schiene", "-ui-pass-strong", "-ui-track", ELEMENT),
            new Combination("Notenband 6.0 gegen Schiene", "-ui-pass-deep", "-ui-track", ELEMENT),
            new Combination("Notenband ungenuegend gegen Schiene", "-ui-fail", "-ui-track", ELEMENT),
            new Combination("Grenzlinie 4.0 im Diagramm", "-ui-text", "-ui-bg-surface", ELEMENT),
            new Combination("Abzeichen Note ausstehend", "-ui-warn", "-ui-warn-wash", TEXT),
            new Combination("Link Pruefung eintragen", "-ui-accent", "-ui-bg-surface", TEXT),
            new Combination("Gewichtung der geplanten Pruefung", "-ui-text-tertiary", "-ui-bg-surface", TEXT));

    @Test
    @DisplayName("Beide Paletten führen genau dieselben Variablen")
    void bothPalettesDefineTheSameVariables() {
        Set<String> light = new TreeSet<>(palette(LIGHT_CSS).keySet());
        Set<String> dark = new TreeSet<>(palette(DARK_CSS).keySet());
        assertEquals(light, dark,
                "theme.css und theme-dark.css müssen dieselben Variablen definieren");
    }

    @Test
    @DisplayName("app.css enthält keinen einzigen Farbwert")
    void appStylesheetHasNoColourLiterals() {
        String css = COMMENT.matcher(read(APP_CSS)).replaceAll("");
        Matcher matcher = LITERAL_COLOUR.matcher(css);
        List<String> found = new ArrayList<>();
        while (matcher.find()) {
            found.add(matcher.group());
        }
        assertTrue(found.isEmpty(),
                "Farbwerte gehören in die Theme-Dateien, gefunden in app.css: " + found);
    }

    @Test
    @DisplayName("Jede in app.css verwendete Farbvariable ist definiert")
    void everyReferencedVariableExists() {
        Map<String, String> light = palette(LIGHT_CSS);
        String css = COMMENT.matcher(read(APP_CSS)).replaceAll("");
        Set<String> missing = new TreeSet<>();
        Matcher matcher = REFERENCE.matcher(css);
        while (matcher.find()) {
            if (!light.containsKey(matcher.group())) {
                missing.add(matcher.group());
            }
        }
        assertTrue(missing.isEmpty(), "In den Theme-Dateien fehlen: " + missing);
    }

    @Test
    @DisplayName("Helles Erscheinungsbild erfüllt WCAG AA")
    void lightAppearanceMeetsWcagAa() {
        assertContrast("hell", palette(LIGHT_CSS));
    }

    @Test
    @DisplayName("Dunkles Erscheinungsbild erfüllt WCAG AA")
    void darkAppearanceMeetsWcagAa() {
        Map<String, String> palette = palette(LIGHT_CSS);
        palette.putAll(palette(DARK_CSS));
        assertContrast("dunkel", palette);
    }

    private static void assertContrast(String appearance, Map<String, String> palette) {
        List<String> failures = new ArrayList<>();
        for (Combination combination : COMBINATIONS) {
            double[] background = colour(combination.background(), palette);
            double[] foreground = over(colour(combination.foreground(), palette), background);
            double ratio = contrast(foreground, background);
            if (ratio + 0.005 < combination.minimum()) {
                failures.add(String.format("%s: %.2f:1 statt %.1f:1",
                        combination.what(), ratio, combination.minimum()));
            }
        }
        if (!failures.isEmpty()) {
            fail("Zu wenig Kontrast im Erscheinungsbild " + appearance + ":\n  "
                    + String.join("\n  ", failures));
        }
    }

    /** Liest die Variablen einer Theme-Datei in der Reihenfolge ihrer Definition. */
    private static Map<String, String> palette(String resource) {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = VARIABLE.matcher(read(resource));
        while (matcher.find()) {
            values.put(matcher.group(1), matcher.group(2).trim());
        }
        assertTrue(values.size() > 20, "Palette " + resource + " wirkt unvollständig");
        return values;
    }

    /** Löst eine Variable oder einen Farbwert in R, G, B und Deckkraft auf. */
    private static double[] colour(String value, Map<String, String> palette) {
        String current = value.trim();
        for (int step = 0; step < 8 && current.startsWith("-ui-"); step++) {
            String resolved = palette.get(current);
            if (resolved == null) {
                fail("Farbvariable " + current + " ist nicht definiert");
            }
            current = resolved.trim();
        }
        if (current.matches("#[0-9a-fA-F]{6}")) {
            int packed = Integer.parseInt(current.substring(1), 16);
            return new double[] {(packed >> 16) & 0xFF, (packed >> 8) & 0xFF, packed & 0xFF, 1};
        }
        Matcher rgba = Pattern.compile("rgba?\\(([^)]+)\\)").matcher(current);
        if (rgba.matches()) {
            String[] parts = rgba.group(1).split(",");
            return new double[] {
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()),
                    parts.length > 3 ? Double.parseDouble(parts[3].trim()) : 1};
        }
        return fail("Unbekannter Farbwert: " + value + " -> " + current);
    }

    /** Legt eine teiltransparente Farbe über ihren Untergrund. */
    private static double[] over(double[] foreground, double[] background) {
        double alpha = foreground[3];
        if (alpha >= 1) {
            return foreground;
        }
        return new double[] {
                foreground[0] * alpha + background[0] * (1 - alpha),
                foreground[1] * alpha + background[1] * (1 - alpha),
                foreground[2] * alpha + background[2] * (1 - alpha),
                1};
    }

    private static double contrast(double[] first, double[] second) {
        double one = luminance(first);
        double other = luminance(second);
        double lighter = Math.max(one, other);
        double darker = Math.min(one, other);
        return (lighter + 0.05) / (darker + 0.05);
    }

    /** Relative Luminanz nach WCAG 2.1. */
    private static double luminance(double[] rgb) {
        return 0.2126 * channel(rgb[0]) + 0.7152 * channel(rgb[1]) + 0.0722 * channel(rgb[2]);
    }

    private static double channel(double value) {
        double scaled = value / 255.0;
        return scaled <= 0.03928 ? scaled / 12.92 : Math.pow((scaled + 0.055) / 1.055, 2.4);
    }

    private static String read(String resource) {
        try (InputStream stream = ThemeContrastTest.class.getResourceAsStream(resource)) {
            if (stream == null) {
                return fail("Stylesheet fehlt: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return fail("Stylesheet nicht lesbar: " + resource, e);
        }
    }
}
