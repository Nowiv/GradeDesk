package ch.gradedesk.i18n;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Die Texte der Anwendung in der eingestellten Sprache.
 *
 * <p>Alle sichtbaren Zeichenketten stehen in den Textbündeln unter
 * {@code /ch/gradedesk/i18n/}. Im Code und in den FXML-Dateien steht nie ein
 * fertiger Satz, sondern immer ein Schlüssel. Dadurch fällt beim Hinzufügen
 * einer Sprache keine Suche durch den Quelltext an.</p>
 *
 * <p>Die eingestellte Sprache ist eine beobachtbare Property. Wer darauf hört,
 * baut sich beim Wechsel neu auf – das UI lädt seine Ansichten dann mit dem
 * neuen Bündel erneut.</p>
 */
public final class I18n {

    private static final String BUNDLE = "ch.gradedesk.i18n.messages";

    private static final ObjectProperty<Language> LANGUAGE =
            new SimpleObjectProperty<>(I18n.class, "language", Language.fromSystem());

    private static ResourceBundle bundle = load(LANGUAGE.get());

    static {
        LANGUAGE.addListener((observable, previous, current) -> bundle = load(current));
    }

    private I18n() {
    }

    private static ResourceBundle load(Language language) {
        return ResourceBundle.getBundle(BUNDLE, language.locale());
    }

    /** Die eingestellte Sprache. */
    public static ReadOnlyObjectProperty<Language> languageProperty() {
        return LANGUAGE;
    }

    public static Language getLanguage() {
        return LANGUAGE.get();
    }

    /** Wechselt die Sprache. Hörer auf {@link #languageProperty()} bauen sich danach neu auf. */
    public static void setLanguage(Language language) {
        if (language != null && language != LANGUAGE.get()) {
            LANGUAGE.set(language);
        }
    }

    /** Das Textbündel der eingestellten Sprache, z.B. für {@code FXMLLoader}. */
    public static ResourceBundle bundle() {
        return bundle;
    }

    /**
     * Der übersetzte Text zu einem Schlüssel.
     *
     * @param key       Schlüssel aus dem Textbündel
     * @param arguments Werte für die Platzhalter {0}, {1}, …
     * @return der Text, oder der Schlüssel in spitzen Klammern, falls er fehlt
     */
    public static String t(String key, Object... arguments) {
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException e) {
            // Sichtbar, aber nicht abstürzend: ein fehlender Schlüssel soll auffallen.
            return "<" + key + ">";
        }
        if (arguments == null || arguments.length == 0) {
            return pattern;
        }
        return new MessageFormat(pattern, LANGUAGE.get().locale()).format(arguments);
    }
}
