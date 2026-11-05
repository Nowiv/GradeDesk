package ch.gradedesk.ui.util;

import javafx.scene.Group;
import javafx.scene.shape.SVGPath;

/**
 * Die Symbole der Anwendung als selbst gezeichnete SVG-Pfade.
 *
 * <p>Alle Pfade liegen auf einem 24er-Raster, sind durchgehend gefüllt und
 * verwenden dieselbe Eckrundung. Bewusst keine Emoji und keine Unicode-Zeichen
 * als Symbolersatz: die würden je nach Betriebssystem anders aussehen.</p>
 */
public final class Icons {

    /** Übersicht: vier Felder. */
    public static final String OVERVIEW =
            "M5.5 4h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4A1.5 1.5 0 0 1 4 9.5v-4A1.5 1.5 0 0 1 5.5 4z"
            + "M14.5 4h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4A1.5 1.5 0 0 1 13 9.5v-4A1.5 1.5 0 0 1 14.5 4z"
            + "M5.5 13h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4A1.5 1.5 0 0 1 4 18.5v-4A1.5 1.5 0 0 1 5.5 13z"
            + "M14.5 13h4a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-4a1.5 1.5 0 0 1-1.5-1.5v-4a1.5 1.5 0 0 1 1.5-1.5z";

    /** Semester und Module: gestapelte Zeilen. */
    public static final String MODULES =
            "M6 5h12a2 2 0 0 1 0 4H6a2 2 0 0 1 0-4z"
            + "M6 10h12a2 2 0 0 1 0 4H6a2 2 0 0 1 0-4z"
            + "M6 15h6a2 2 0 0 1 0 4H6a2 2 0 0 1 0-4z";

    /** Diagramm: drei Säulen. */
    public static final String CHART =
            "M5.5 13h1a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-1A1.5 1.5 0 0 1 4 18.5v-4A1.5 1.5 0 0 1 5.5 13z"
            + "M11.5 8h1a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1-1.5 1.5h-1A1.5 1.5 0 0 1 10 18.5v-9A1.5 1.5 0 0 1 11.5 8z"
            + "M17.5 4h1a1.5 1.5 0 0 1 1.5 1.5v13a1.5 1.5 0 0 1-1.5 1.5h-1a1.5 1.5 0 0 1-1.5-1.5v-13A1.5 1.5 0 0 1 17.5 4z";

    /** Einstellungen: zwei Schieberegler. */
    public static final String SETTINGS =
            "M5 7h14a1 1 0 0 1 0 2H5a1 1 0 0 1 0-2z"
            + "M9 5a3 3 0 1 0 0 6 3 3 0 0 0 0-6z"
            + "M5 15h14a1 1 0 0 1 0 2H5a1 1 0 0 1 0-2z"
            + "M15 13a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";

    /** Plus: etwas hinzufügen. */
    public static final String PLUS =
            "M12 4.5a1.5 1.5 0 0 1 1.5 1.5v5h5a1.5 1.5 0 0 1 0 3h-5v5a1.5 1.5 0 0 1-3 0v-5h-5a1.5 1.5 0 0 1 0-3h5V6a1.5 1.5 0 0 1 1.5-1.5z";

    /** Stift: bearbeiten. */
    public static final String EDIT =
            "M16.2 3.6a2 2 0 0 1 2.8 0l1.4 1.4a2 2 0 0 1 0 2.8l-1.3 1.3-4.2-4.2 1.3-1.3z"
            + "M13.5 6.3l4.2 4.2-8.1 8.1-5.1.9.9-5.1 8.1-8.1z";

    /** Papierkorb: löschen. */
    public static final String DELETE =
            "M9.5 3h5a1.5 1.5 0 0 1 1.5 1.5V5h3.5a1 1 0 0 1 0 2h-15a1 1 0 0 1 0-2H8V4.5A1.5 1.5 0 0 1 9.5 3z"
            + "M6 8.5h12l-.8 10.2A2.4 2.4 0 0 1 14.8 21H9.2a2.4 2.4 0 0 1-2.4-2.3L6 8.5z";

    /** Ausrufezeichen im Kreis: Hinweis. */
    public static final String ALERT =
            "M12 2.5a9.5 9.5 0 1 0 0 19 9.5 9.5 0 0 0 0-19z"
            + "M12.9 6.8l-.3 7.1h-1.2l-.3-7.1h1.8z"
            + "M12 15.6a1.3 1.3 0 1 1 0 2.6 1.3 1.3 0 0 1 0-2.6z";

    /** Kopieren. */
    public static final String COPY =
            "M9 3h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"
            + "M5.5 7H6v10a2.5 2.5 0 0 0 2.5 2.5H16v.5a2 2 0 0 1-2 2H7a3.5 3.5 0 0 1-3.5-3.5V9a2 2 0 0 1 2-2z";

    /** Schloss: lokale Speicherung. */
    public static final String LOCK =
            "M12 2.5A4.5 4.5 0 0 0 7.5 7v2.5H9V7a3 3 0 0 1 6 0v2.5h1.5V7A4.5 4.5 0 0 0 12 2.5z"
            + "M6.5 10h11A1.5 1.5 0 0 1 19 11.5v8A1.5 1.5 0 0 1 17.5 21h-11A1.5 1.5 0 0 1 5 19.5v-8A1.5 1.5 0 0 1 6.5 10z";

    private Icons() {
    }

    /**
     * Erzeugt ein Symbol in der gewünschten Kantenlänge.
     *
     * <p>Der Pfad steckt in einer {@link Group}, damit die Skalierung auch in die
     * Layoutgrösse einfliesst und das Symbol im Fluss korrekt Platz beansprucht.</p>
     *
     * @param path       eine der Pfadkonstanten dieser Klasse
     * @param size       Kantenlänge in Pixeln (das Raster ist 24 px gross)
     * @param styleClass zusätzliche Stilklassen für die Einfärbung
     */
    public static Group create(String path, double size, String... styleClass) {
        SVGPath shape = new SVGPath();
        shape.setContent(path);
        double scale = size / 24.0;
        shape.setScaleX(scale);
        shape.setScaleY(scale);
        shape.getStyleClass().add("icon");
        shape.getStyleClass().addAll(styleClass);

        Group icon = new Group(shape);
        icon.setMouseTransparent(true);
        return icon;
    }
}
