package ch.gradedesk.ui.control;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.i18n.Language;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/**
 * Der Sprachumschalter: ein segmentiertes Bedienelement mit einer Taste je Sprache.
 *
 * <p>Bewusste Entscheide:</p>
 * <ul>
 *   <li><b>Jede Sprache nennt sich selbst.</b> «Deutsch» und «English» stehen in
 *       ihrer eigenen Sprache. Wer die Anwendung in einer fremden Sprache
 *       vorfindet, erkennt die eigene trotzdem.</li>
 *   <li><b>Keine Flaggen.</b> Eine Flagge bezeichnet ein Land, nicht eine
 *       Sprache – Deutsch wird in mehreren Ländern gesprochen.</li>
 *   <li><b>Beide Sprachen sind sichtbar</b>, nicht hinter einem Auswahlfeld
 *       versteckt. Bei zwei Sprachen braucht der Wechsel so nur einen Klick,
 *       und man sieht sofort, was angeboten wird.</li>
 *   <li><b>Die Wahl wirkt sofort</b> und wird gespeichert. Es gibt kein
 *       «Übernehmen», das man vergessen könnte.</li>
 * </ul>
 *
 * <p>Die aktive Taste bleibt gedrückt; ein erneuter Klick darauf hebt die
 * Auswahl nicht auf, weil «keine Sprache» kein sinnvoller Zustand wäre.</p>
 */
public final class LanguageSwitch extends HBox {

    private final ToggleGroup group = new ToggleGroup();

    public LanguageSwitch() {
        getStyleClass().add("language-switch");
        setFocusTraversable(false);

        for (Language language : Language.values()) {
            ToggleButton button = new ToggleButton(language.endonym());
            button.getStyleClass().add("language-switch__option");
            button.setUserData(language);
            button.setToggleGroup(group);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAccessibleText(language.endonym());
            setHgrow(button, javafx.scene.layout.Priority.ALWAYS);
            getChildren().add(button);
        }

        select(I18n.getLanguage());

        group.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                // Die aktive Sprache lässt sich nicht abwählen.
                group.selectToggle(previous);
                return;
            }
            I18n.setLanguage((Language) selected.getUserData());
        });

        I18n.languageProperty().addListener((observable, previous, current) -> select(current));
    }

    private void select(Language language) {
        for (Toggle toggle : group.getToggles()) {
            if (toggle.getUserData() == language) {
                group.selectToggle(toggle);
                return;
            }
        }
    }
}
