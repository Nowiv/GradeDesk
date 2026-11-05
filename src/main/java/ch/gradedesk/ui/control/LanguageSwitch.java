package ch.gradedesk.ui.control;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.i18n.Language;

import java.util.List;

/**
 * Der Sprachumschalter.
 *
 * <p>Bewusste Entscheide:</p>
 * <ul>
 *   <li><b>Jede Sprache nennt sich selbst.</b> «Deutsch» und «English» stehen in
 *       ihrer eigenen Sprache. Wer die Anwendung in einer fremden Sprache
 *       vorfindet, erkennt die eigene trotzdem.</li>
 *   <li><b>Keine Flaggen.</b> Eine Flagge bezeichnet ein Land, nicht eine
 *       Sprache – Deutsch wird in mehreren Ländern gesprochen.</li>
 *   <li><b>Beide Sprachen sichtbar</b>, nicht in einem Auswahlfeld versteckt.</li>
 *   <li><b>Die Wahl wirkt sofort</b> und wird gemerkt. Es gibt kein
 *       «Übernehmen», das man vergessen könnte.</li>
 * </ul>
 */
public final class LanguageSwitch extends SegmentedControl<Language> {

    public LanguageSwitch() {
        super(List.of(Language.values()), Language::endonym, I18n.getLanguage(), I18n::setLanguage);
        I18n.languageProperty().addListener((observable, previous, current) -> select(current));
    }
}
