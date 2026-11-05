package ch.notenrechner.ui;

import ch.notenrechner.ui.util.Icons;
import ch.notenrechner.ui.view.ChartController;
import ch.notenrechner.ui.view.OverviewController;
import ch.notenrechner.ui.view.SemestersController;
import ch.notenrechner.ui.view.SettingsController;

import java.util.List;

/**
 * Die Ansichten der Anwendung in der Reihenfolge der Seitenleiste.
 *
 * <p><b>Erweiterungspunkt.</b> Eine zusätzliche Ansicht braucht drei Dinge:
 * eine FXML-Datei unter {@code /ch/notenrechner/ui/view/}, einen Controller, der
 * {@link ViewController} umsetzt, und eine weitere Zeile in dieser Liste. Die
 * Seitenleiste, das Laden und der Ansichtswechsel entstehen daraus von selbst.</p>
 */
public final class Views {

    private static final String BASE = "/ch/notenrechner/ui/view/";

    private Views() {
    }

    public static List<NavigationEntry> entries() {
        return List.of(
                new NavigationEntry("overview", "Übersicht", Icons.OVERVIEW,
                        BASE + "overview-view.fxml", OverviewController::new),
                new NavigationEntry("semesters", "Semester & Module", Icons.MODULES,
                        BASE + "semesters-view.fxml", SemestersController::new),
                new NavigationEntry("chart", "Diagramm", Icons.CHART,
                        BASE + "chart-view.fxml", ChartController::new),
                new NavigationEntry("settings", "Einstellungen", Icons.SETTINGS,
                        BASE + "settings-view.fxml", SettingsController::new));
    }
}
