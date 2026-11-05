package ch.gradedesk.ui.util;

import javafx.scene.Group;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Die Symbole der Anwendung als selbst gezeichnete SVG-Pfade.
 *
 * <p>Linien-Stil auf einem 24er-Raster, durchgehend 1.7 px stark, mit runden
 * Enden und Ecken. Das ist der Charakter von Apples SF Symbols, ohne deren
 * Dateien zu verwenden: SF Symbols sind lizenzrechtlich an Apple-Plattformen
 * gebunden und duerfen hier nicht mitgeliefert werden.</p>
 *
 * <p>Gezeichnet wird mit Kontur statt Fuellung. Die Farbe kommt deshalb aus
 * {@code -fx-stroke}, nicht aus {@code -fx-fill} – siehe {@code .icon} in
 * app.css. Bewusst keine Emoji und keine Unicode-Zeichen als Symbolersatz:
 * die wuerden je nach Betriebssystem anders aussehen.</p>
 */
public final class Icons {

    /** Staerke der Linien auf dem 24er-Raster. */
    private static final double STROKE_WIDTH = 1.7;

    /** Uebersicht: vier Felder. */
    public static final String OVERVIEW =
            "M4.75 4.75h5.5v5.5h-5.5z"
            + "M13.75 4.75h5.5v5.5h-5.5z"
            + "M4.75 13.75h5.5v5.5h-5.5z"
            + "M13.75 13.75h5.5v5.5h-5.5z";

    /** Semester und Module: gestapelte Zeilen. */
    public static final String MODULES =
            "M4.75 6.5h14.5"
            + "M4.75 12h14.5"
            + "M4.75 17.5h8.5";

    /** Diagramm: drei Saeulen auf einer Grundlinie. */
    public static final String CHART =
            "M4.75 19.25h14.5"
            + "M7.5 19.25v-5"
            + "M12 19.25v-9.5"
            + "M16.5 19.25v-7";

    /** Einstellungen: zwei Schieberegler. */
    public static final String SETTINGS =
            "M4.75 8.5h14.5"
            + "M4.75 15.5h14.5"
            + "M9.5 6.6a1.9 1.9 0 1 0 0 3.8 1.9 1.9 0 0 0 0-3.8z"
            + "M15 13.6a1.9 1.9 0 1 0 0 3.8 1.9 1.9 0 0 0 0-3.8z";

    /** Plus: etwas hinzufuegen. */
    public static final String PLUS =
            "M12 5.5v13"
            + "M5.5 12h13";

    /** Stift: bearbeiten. */
    public static final String EDIT =
            "M16.4 4.6a1.9 1.9 0 0 1 2.7 2.7l-9.6 9.6-3.6.9.9-3.6 9.6-9.6z"
            + "M14.8 6.2l3 3";

    /** Papierkorb: loeschen. */
    public static final String DELETE =
            "M5.5 7h13"
            + "M9.75 7V5.6a1 1 0 0 1 1-1h2.5a1 1 0 0 1 1 1V7"
            + "M7.3 7l.75 11.4a1.6 1.6 0 0 0 1.6 1.5h4.7a1.6 1.6 0 0 0 1.6-1.5L16.7 7"
            + "M10.4 10.5v6"
            + "M13.6 10.5v6";

    /** Ausrufezeichen im Kreis: Hinweis. */
    public static final String ALERT =
            "M12 4.4a7.6 7.6 0 1 0 0 15.2 7.6 7.6 0 0 0 0-15.2z"
            + "M12 8.2v4.6"
            + "M12 15.8v.1";

    /** Kopieren. */
    public static final String COPY =
            "M9.6 4.75h7.9a1.9 1.9 0 0 1 1.9 1.9v7.9a1.9 1.9 0 0 1-1.9 1.9h-7.9a1.9 1.9 0 0 1-1.9-1.9v-7.9a1.9 1.9 0 0 1 1.9-1.9z"
            + "M15.4 19.25H6.5a1.9 1.9 0 0 1-1.9-1.9V8.5";

    /** Schloss: lokale Speicherung. */
    public static final String LOCK =
            "M7.6 10.3V7.7a4.4 4.4 0 0 1 8.8 0v2.6"
            + "M6.9 10.3h10.2a1.6 1.6 0 0 1 1.6 1.6v6.1a1.6 1.6 0 0 1-1.6 1.6H6.9a1.6 1.6 0 0 1-1.6-1.6v-6.1a1.6 1.6 0 0 1 1.6-1.6z";

    private Icons() {
    }

    /**
     * Erzeugt ein Symbol in der gewuenschten Kantenlaenge.
     *
     * <p>Der Pfad steckt in einer {@link Group}, damit die Skalierung auch in
     * die Layoutgroesse einfliesst und das Symbol im Fluss korrekt Platz
     * beansprucht.</p>
     *
     * @param path       eine der Pfadkonstanten dieser Klasse
     * @param size       Kantenlaenge in Pixeln (das Raster ist 24 px gross)
     * @param styleClass zusaetzliche Stilklassen fuer die Einfaerbung
     */
    public static Group create(String path, double size, String... styleClass) {
        SVGPath shape = new SVGPath();
        shape.setContent(path);

        // Kontur statt Fuellung: das ergibt den Linien-Stil.
        shape.setFill(null);
        shape.setStrokeWidth(STROKE_WIDTH);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);

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
