package ch.gradedesk.i18n;

import java.util.Locale;
import java.util.Optional;

/**
 * Die Sprachen, die GradeDesk anbietet.
 *
 * <p>Jede Sprache nennt sich selbst in ihrer eigenen Schreibweise
 * («Deutsch», «English»). Das ist bewusst: wer die Anwendung in einer Sprache
 * vorfindet, die er nicht versteht, erkennt seine eigene trotzdem wieder.
 * Aus demselben Grund stehen hier keine Flaggen – eine Flagge bezeichnet ein
 * Land, keine Sprache.</p>
 */
public enum Language {

    /** Deutsch (Schweiz). */
    DE("Deutsch", Locale.forLanguageTag("de-CH")),

    /** Englisch. */
    EN("English", Locale.ENGLISH);

    private final String endonym;
    private final Locale locale;

    Language(String endonym, Locale locale) {
        this.endonym = endonym;
        this.locale = locale;
    }

    /** Der Name der Sprache in dieser Sprache selbst. */
    public String endonym() {
        return endonym;
    }

    /** Die zugehörige Locale für Textbündel und Datumsformate. */
    public Locale locale() {
        return locale;
    }

    /** Die Kennung, wie sie in den Einstellungen gespeichert wird. */
    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Sucht eine Sprache anhand der gespeicherten Kennung. */
    public static Optional<Language> fromTag(String tag) {
        if (tag == null) {
            return Optional.empty();
        }
        for (Language language : values()) {
            if (language.tag().equalsIgnoreCase(tag)) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }

    /**
     * Die Sprache des Betriebssystems, sofern sie angeboten wird.
     * Sonst Englisch – die breitere Verständlichkeit gewinnt im Zweifel.
     */
    public static Language fromSystem() {
        String systemLanguage = Locale.getDefault().getLanguage();
        return DE.locale().getLanguage().equals(systemLanguage) ? DE : EN;
    }
}
