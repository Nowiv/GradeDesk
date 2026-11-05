/**
 * GradeDesk: lokale Desktop-Anwendung zum Erfassen und Auswerten von Schulnoten.
 *
 * <p>Das Modul ist bewusst eng geschnitten: es öffnet nur, was JavaFX für FXML
 * und Jackson für die Serialisierung wirklich braucht. Dadurch lassen sich mit
 * jlink und jpackage schlanke, in sich geschlossene Installer erzeugen.</p>
 */
module ch.gradedesk {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    // Nicht im Code referenziert, aber zwingend: jdk.crypto.ec liefert den
    // Sicherheitsanbieter SunEC. Ohne ein "requires" lässt jlink dieses Modul
    // weg, weil es nur über einen Dienst gefunden wird und nicht über eine
    // Abhängigkeit. In der fertigen Runtime scheitert dann jeder HTTPS-Aufbau
    // zu api.github.com mit "handshake_failure", weil der Server einen
    // Schlüsselaustausch über elliptische Kurven verlangt.
    requires jdk.crypto.ec;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    exports ch.gradedesk;
    exports ch.gradedesk.i18n;
    exports ch.gradedesk.model;
    exports ch.gradedesk.service;
    exports ch.gradedesk.persistence;
    exports ch.gradedesk.persistence.dto;
    exports ch.gradedesk.ui;
    exports ch.gradedesk.ui.chart;
    exports ch.gradedesk.ui.control;
    exports ch.gradedesk.ui.util;
    exports ch.gradedesk.ui.view;
    exports ch.gradedesk.update;

    // FXMLLoader setzt die mit @FXML versehenen Felder per Reflection.
    opens ch.gradedesk.ui to javafx.fxml;
    opens ch.gradedesk.ui.view to javafx.fxml;

    // Jackson liest und schreibt die Records im DTO-Paket per Reflection.
    opens ch.gradedesk.persistence.dto to com.fasterxml.jackson.databind;
}
