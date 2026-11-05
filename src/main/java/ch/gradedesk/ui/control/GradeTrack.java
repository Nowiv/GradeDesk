package ch.gradedesk.ui.control;

import ch.gradedesk.service.GradeScale;
import ch.gradedesk.ui.util.Formats;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Das Notenband: die Skala 1–6 als waagrechte Strecke.
 *
 * <p>Teilstriche sitzen bei jeder ganzen Note, die Genügend-Grenze bei 4.0 ist
 * als kräftige Markierung hervorgehoben, und die erreichte Note erscheint als
 * gefüllter Abschnitt mit einem Punkt an ihrer Position.</p>
 *
 * <p>Der Status hängt damit nicht allein an der Farbe, sondern auch an der
 * Position auf der Strecke – lesbar auch mit einer Rot-Grün-Sehschwäche.
 * Dieselbe Geometrie verwendet das Säulendiagramm senkrecht.</p>
 */
public final class GradeTrack extends Region {

    private static final double RAIL_HEIGHT = 8;
    private static final double TICK_WIDTH = 1;
    private static final double TICK_HEIGHT = 12;
    private static final double THRESHOLD_WIDTH = 2;
    private static final double MARKER_SIZE = 12;

    private final Region rail = new Region();
    private final Region fill = new Region();
    private final Region threshold = new Region();
    private final Region marker = new Region();
    private final List<Region> ticks = new ArrayList<>();

    private final ObjectProperty<BigDecimal> grade = new SimpleObjectProperty<>(this, "grade");

    private String appliedFillClass;

    public GradeTrack() {
        getStyleClass().add("grade-track");

        rail.getStyleClass().add("grade-track__rail");
        fill.getStyleClass().add("grade-track__fill");
        threshold.getStyleClass().add("grade-track__threshold");
        marker.getStyleClass().add("grade-track__marker");

        getChildren().addAll(rail, fill);
        // Teilstriche bei 2, 3 und 5: die Enden 1 und 6 markiert das Band selbst,
        // die 4 bekommt die kräftige Grenzmarkierung.
        for (int value = 2; value <= 5; value++) {
            if (value == 4) {
                continue;
            }
            Region tick = new Region();
            tick.getStyleClass().add("grade-track__tick");
            tick.setUserData(BigDecimal.valueOf(value));
            ticks.add(tick);
            getChildren().add(tick);
        }
        getChildren().addAll(threshold, marker);

        grade.addListener((observable, oldValue, newValue) -> onGradeChanged(newValue));
        onGradeChanged(null);
    }

    /** Die dargestellte Modulnote. {@code null} zeigt ein leeres Band. */
    public ObjectProperty<BigDecimal> gradeProperty() {
        return grade;
    }

    public void setGrade(BigDecimal value) {
        grade.set(value);
    }

    public BigDecimal getGrade() {
        return grade.get();
    }

    private void onGradeChanged(BigDecimal value) {
        boolean present = value != null;
        fill.setVisible(present);
        marker.setVisible(present);

        if (appliedFillClass != null) {
            fill.getStyleClass().remove(appliedFillClass);
            marker.getStyleClass().remove(appliedFillClass);
            appliedFillClass = null;
        }
        if (present) {
            appliedFillClass = Formats.gradeColorClass("grade-fill--", value);
            fill.getStyleClass().add(appliedFillClass);
            marker.getStyleClass().add(appliedFillClass);
        }

        setAccessibleText(present
                ? "Modulnote " + Formats.grade(value) + ", " + Formats.statusText(value)
                + ", auf der Skala 1 bis 6"
                : "Keine Modulnote vorhanden");
        requestLayout();
    }

    /** Wandelt eine Note in eine x-Position innerhalb des Bandes um. */
    private double positionOf(BigDecimal value, double width) {
        double min = GradeScale.MIN.doubleValue();
        double max = GradeScale.MAX.doubleValue();
        double ratio = (value.doubleValue() - min) / (max - min);
        return Math.max(0, Math.min(1, ratio)) * width;
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        double railY = Math.round((height - RAIL_HEIGHT) / 2.0);
        rail.resizeRelocate(0, railY, width, RAIL_HEIGHT);

        for (Region tick : ticks) {
            BigDecimal value = (BigDecimal) tick.getUserData();
            double x = positionOf(value, width);
            tick.resizeRelocate(Math.round(x - TICK_WIDTH / 2.0),
                    Math.round((height - TICK_HEIGHT) / 2.0), TICK_WIDTH, TICK_HEIGHT);
        }

        double thresholdX = positionOf(GradeScale.PASSING, width);
        threshold.resizeRelocate(Math.round(thresholdX - THRESHOLD_WIDTH / 2.0), 0,
                THRESHOLD_WIDTH, height);

        BigDecimal value = getGrade();
        if (value == null) {
            return;
        }

        double gradeX = positionOf(value, width);
        fill.resizeRelocate(0, railY, Math.max(gradeX, 1), RAIL_HEIGHT);

        // Der Punkt bleibt vollständig im Band, auch bei 1.0 und 6.0.
        double markerX = Math.max(0, Math.min(width - MARKER_SIZE, gradeX - MARKER_SIZE / 2.0));
        marker.resizeRelocate(Math.round(markerX), Math.round((height - MARKER_SIZE) / 2.0),
                MARKER_SIZE, MARKER_SIZE);
    }
}
