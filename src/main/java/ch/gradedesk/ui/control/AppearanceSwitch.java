package ch.gradedesk.ui.control;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.ui.util.Appearance;
import ch.gradedesk.ui.util.Theme;

import java.util.List;

/**
 * Der Umschalter zwischen hellem und dunklem Erscheinungsbild.
 *
 * <p>Dieselbe Figur wie beim Sprachumschalter – gleiche Aufgabe, gleiches
 * Bedienelement. Die Wahl wirkt sofort, weil nur eine Stilklasse an der Wurzel
 * der Szene getauscht wird.</p>
 */
public final class AppearanceSwitch extends SegmentedControl<Appearance> {

    public AppearanceSwitch() {
        super(List.of(Appearance.values()),
                appearance -> I18n.t(appearance.labelKey()),
                Theme.getAppearance(),
                Theme::setAppearance);
        Theme.appearanceProperty().addListener((observable, previous, current) -> select(current));
    }
}
