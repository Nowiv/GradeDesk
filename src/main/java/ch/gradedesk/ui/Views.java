package ch.gradedesk.ui;

import ch.gradedesk.ui.util.Icons;
import ch.gradedesk.ui.view.ChartController;
import ch.gradedesk.ui.view.OverviewController;
import ch.gradedesk.ui.view.SemestersController;
import ch.gradedesk.ui.view.SettingsController;

import java.util.List;

/**
 * Die Ansichten der Anwendung in der Reihenfolge der Seitenleiste.
 *
 * <p><b>Erweiterungspunkt.</b> Eine zusätzliche Ansicht braucht vier Dinge:
 * eine FXML-Datei unter {@code /ch/gradedesk/ui/view/}, einen Controller, der
 * {@link ViewController} umsetzt, je einen Eintrag in den beiden Textbündeln
 * und eine weitere Zeile in dieser Liste. Die Seitenleiste, das Laden, der
 * Ansichtswechsel und der Sprachwechsel entstehen daraus von selbst.</p>
 */
public final class Views {

    private static final String BASE = "/ch/gradedesk/ui/view/";

    private Views() {
    }

    public static List<NavigationEntry> entries() {
        return List.of(
                new NavigationEntry("overview", "nav.overview", Icons.OVERVIEW,
                        BASE + "overview-view.fxml", OverviewController::new),
                new NavigationEntry("semesters", "nav.semesters", Icons.MODULES,
                        BASE + "semesters-view.fxml", SemestersController::new),
                new NavigationEntry("chart", "nav.chart", Icons.CHART,
                        BASE + "chart-view.fxml", ChartController::new),
                new NavigationEntry("settings", "nav.settings", Icons.SETTINGS,
                        BASE + "settings-view.fxml", SettingsController::new));
    }
}
