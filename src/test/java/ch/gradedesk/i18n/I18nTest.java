package ch.gradedesk.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nTest {

    private static final String BUNDLE = "ch.gradedesk.i18n.messages";

    private final Language original = I18n.getLanguage();

    @AfterEach
    void restoreLanguage() {
        I18n.setLanguage(original);
    }

    @Test
    @DisplayName("Beide Sprachen kennen genau dieselben Schlüssel")
    void bundlesHaveTheSameKeys() {
        Set<String> english = new TreeSet<>(
                ResourceBundle.getBundle(BUNDLE, Locale.ROOT).keySet());
        Set<String> german = new TreeSet<>(
                ResourceBundle.getBundle(BUNDLE, Locale.GERMAN).keySet());

        Set<String> missingInGerman = new TreeSet<>(english);
        missingInGerman.removeAll(german);
        Set<String> missingInEnglish = new TreeSet<>(german);
        missingInEnglish.removeAll(english);

        assertTrue(missingInGerman.isEmpty(), "Fehlt auf Deutsch: " + missingInGerman);
        assertTrue(missingInEnglish.isEmpty(), "Fehlt auf Englisch: " + missingInEnglish);
    }

    @Test
    @DisplayName("Kein Text ist leer geblieben")
    void noBundleEntryIsEmpty() {
        for (Language language : Language.values()) {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, language.locale());
            for (String key : bundle.keySet()) {
                assertFalse(bundle.getString(key).isBlank(),
                        "Leerer Text: " + key + " (" + language + ")");
            }
        }
    }

    @Test
    @DisplayName("Platzhalter stimmen zwischen den Sprachen überein")
    void placeholdersMatchAcrossLanguages() {
        ResourceBundle english = ResourceBundle.getBundle(BUNDLE, Locale.ROOT);
        ResourceBundle german = ResourceBundle.getBundle(BUNDLE, Locale.GERMAN);

        for (String key : english.keySet()) {
            int expected = new MessageFormat(english.getString(key)).getFormatsByArgumentIndex().length;
            int actual = new MessageFormat(german.getString(key)).getFormatsByArgumentIndex().length;
            assertEquals(expected, actual, "Andere Anzahl Platzhalter bei " + key);
        }
    }

    @Test
    @DisplayName("Englisch fällt nicht auf die Systemsprache zurück")
    void englishDoesNotFallBackToTheSystemLanguage() {
        // ResourceBundle sucht nach der Zielsprache zuerst in der Standardsprache
        // des Rechners und erst danach im Basisbündel. Ohne messages_en.properties
        // liefe Englisch auf einem deutschen System deshalb auf Deutsch hinaus.
        I18n.setLanguage(Language.EN);
        String english = I18n.t("nav.overview");

        I18n.setLanguage(Language.DE);
        String german = I18n.t("nav.overview");

        assertEquals("Overview", english);
        assertEquals("Übersicht", german);
        assertNotEquals(english, german);
    }

    @ParameterizedTest(name = "{0} liefert übersetzte Texte")
    @EnumSource(Language.class)
    void everyLanguageResolvesItsTexts(Language language) {
        I18n.setLanguage(language);

        assertFalse(I18n.t("app.tagline").startsWith("<"), "Schlüssel nicht gefunden");
        assertFalse(I18n.t("overview.title").isBlank());
    }

    @Test
    @DisplayName("Ein fehlender Schlüssel fällt auf, stürzt aber nicht ab")
    void missingKeyIsVisibleButHarmless() {
        assertEquals("<kein.solcher.schluessel>", I18n.t("kein.solcher.schluessel"));
    }

    @Test
    @DisplayName("Mehrzahlformen richten sich nach der Anzahl")
    void pluralFormsFollowTheCount() {
        I18n.setLanguage(Language.DE);
        assertEquals("keine Noten", I18n.t("count.grades", 0));
        assertEquals("1 Note", I18n.t("count.grades", 1));
        assertEquals("3 Noten", I18n.t("count.grades", 3));

        I18n.setLanguage(Language.EN);
        assertEquals("no grades", I18n.t("count.grades", 0));
        assertEquals("1 grade", I18n.t("count.grades", 1));
        assertEquals("3 grades", I18n.t("count.grades", 3));
    }

    @Test
    @DisplayName("Jede Sprache nennt sich in ihrer eigenen Schreibweise")
    void languagesUseTheirOwnName() {
        assertEquals("Deutsch", Language.DE.endonym());
        assertEquals("English", Language.EN.endonym());
        assertEquals("de", Language.DE.tag());
        assertEquals("en", Language.EN.tag());
        assertEquals(Language.DE, Language.fromTag("de").orElseThrow());
        assertEquals(Language.EN, Language.fromTag("EN").orElseThrow());
        assertTrue(Language.fromTag("fr").isEmpty());
        assertTrue(Language.fromTag(null).isEmpty());
    }
}
