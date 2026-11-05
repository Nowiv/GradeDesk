/**
 * Notenrechner: lokale Desktop-Anwendung zum Erfassen und Auswerten von Schulnoten.
 *
 * <p>Das Modul ist bewusst eng geschnitten: es öffnet nur, was JavaFX für FXML
 * und Jackson für die Serialisierung wirklich braucht. Dadurch lassen sich mit
 * jlink und jpackage schlanke, in sich geschlossene Installer erzeugen.</p>
 */
module ch.notenrechner {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    exports ch.notenrechner.model;
    exports ch.notenrechner.service;
}
