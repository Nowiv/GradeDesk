package ch.notenrechner.ui;

import java.util.function.Supplier;

/**
 * Ein Eintrag der Seitenleiste.
 *
 * @param id                eindeutige Kennung, auch für den Programmwechsel der Ansicht
 * @param title             Beschriftung in der Navigation
 * @param iconPath          SVG-Pfad aus {@link ch.notenrechner.ui.util.Icons}
 * @param fxml              Ressourcenpfad der FXML-Datei
 * @param controllerFactory erzeugt den Controller; bewusst ohne Reflection,
 *                          damit das Modul nichts zusätzlich öffnen muss
 */
public record NavigationEntry(
        String id,
        String title,
        String iconPath,
        String fxml,
        Supplier<ViewController> controllerFactory) {
}
