package ch.gradedesk.ui.util;

import java.util.Locale;
import java.util.Optional;

/**
 * Hell oder dunkel – das Erscheinungsbild der Oberfläche.
 *
 * <p>Die Farben beider Varianten sind nicht einfach invertiert, sondern zwei
 * vollständige Farbsätze: {@code theme.css} führt den hellen,
 * {@code theme-dark.css} den dunklen. Umgeschaltet wird, welche Dateien an
 * einer Szene hängen – siehe {@link Theme}.</p>
 *
 * <p>Eine automatische Übernahme vom Betriebssystem gibt es bewusst nicht:
 * JavaFX 21 kennt dafür keine Schnittstelle. Die Wahl ist deshalb ausdrücklich
 * und wird gemerkt.</p>
 *
 * <p>Die Stilklasse unten bezeichnet das Erscheinungsbild an der Wurzel der
 * Szene. Sie trägt keine Farben: Popup-Fenster erben die Stylesheets ihrer
 * Szene, aber nicht deren Stilklassen.</p>
 */
public enum Appearance {

    /** Heller Modus. */
    LIGHT("appearance-light"),

    /** Dunkler Modus. */
    DARK("appearance-dark");

    private final String styleClass;

    Appearance(String styleClass) {
        this.styleClass = styleClass;
    }

    /** Die Stilklasse, die an der Wurzel der Szene gesetzt wird. */
    public String styleClass() {
        return styleClass;
    }

    /** Die Kennung, wie sie in den Einstellungen gespeichert wird. */
    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Schlüssel des Namens im Textbündel. */
    public String labelKey() {
        return "appearance." + tag();
    }

    /** Sucht ein Erscheinungsbild anhand der gespeicherten Kennung. */
    public static Optional<Appearance> fromTag(String tag) {
        if (tag == null) {
            return Optional.empty();
        }
        for (Appearance appearance : values()) {
            if (appearance.tag().equalsIgnoreCase(tag)) {
                return Optional.of(appearance);
            }
        }
        return Optional.empty();
    }
}
