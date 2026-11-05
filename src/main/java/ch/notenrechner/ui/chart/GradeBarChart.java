package ch.notenrechner.ui.chart;

import ch.notenrechner.model.CourseModule;
import ch.notenrechner.model.Semester;
import ch.notenrechner.service.GradeScale;
import ch.notenrechner.ui.util.Formats;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Säulendiagramm der Modulnoten, gruppiert und sortiert nach Semester.
 *
 * <p>Aufbau:</p>
 * <ul>
 *   <li>Eine Säule pro Modul mit Note. Module ohne Noten erscheinen nicht.</li>
 *   <li>Y-Achse fix von 1 bis 6 mit einer waagrechten Linie bei jeder ganzen Note.</li>
 *   <li>Die Genügend-Grenze bei 4.0 ist als durchgezogene, beschriftete Linie
 *       deutlich hervorgehoben.</li>
 *   <li>Gestrichelte Trenner und eine Beschriftung je Semester machen die
 *       Gruppen auf der X-Achse erkennbar.</li>
 *   <li>Die Säulenfarbe folgt der Modulnote, nicht dem Semester.</li>
 * </ul>
 *
 * <p>Die Grenzlinie und die Semestertrenner liegen in einer eigenen Ebene über
 * der Zeichenfläche. Sie werden bei jedem Layoutdurchlauf neu positioniert,
 * damit das Diagramm beim Skalieren des Fensters korrekt bleibt.</p>
 */
public final class GradeBarChart extends StackPane {

    /** Unsichtbares Zeichen, mit dem gleichnamige Module eindeutige Kategorien erhalten. */
    private static final char INVISIBLE_MARKER = '​';

    /** Obergrenze für die Säulenbreite. Ohne sie werden wenige Module zu Klötzen. */
    private static final double MAX_BAR_WIDTH = 72;

    /** Kleinster Abstand zwischen zwei Säulen. */
    private static final double MIN_CATEGORY_GAP = 14;

    private final CategoryAxis xAxis = new CategoryAxis();
    private final NumberAxis yAxis = new NumberAxis(
            GradeScale.MIN.doubleValue(), GradeScale.MAX.doubleValue(), 1);
    private final BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
    private final Pane overlay = new Pane();

    private final Line thresholdLine = new Line();
    private final Text thresholdCaption = new Text("Genügend ab 4.0");
    private final List<Line> dividers = new ArrayList<>();
    private final List<Text> semesterLabels = new ArrayList<>();

    /** Erste und letzte Kategorie je Semester, für Trenner und Beschriftung. */
    private final List<SemesterGroup> groups = new ArrayList<>();

    /**
     * Die Notenfarbe je Datenpunkt.
     *
     * <p>Nötig, weil {@code BarChart} die Stilklassen seiner Säulen beim Aufbau
     * mit {@code setAll} zurücksetzt. Die Farbe wird deshalb bei jedem
     * Layoutdurchlauf erneut gesetzt, falls sie verloren ging.</p>
     */
    private final Map<XYChart.Data<String, Number>, String> barStyleClasses = new LinkedHashMap<>();

    public GradeBarChart() {
        getStyleClass().add("grade-chart-container");

        xAxis.setLabel("Module");
        xAxis.setTickLabelRotation(-35);
        xAxis.setGapStartAndEnd(true);

        yAxis.setLabel("Note");
        yAxis.setAutoRanging(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override
            public String toString(Number value) {
                return String.valueOf(value.intValue());
            }
        });

        chart.getStyleClass().add("grade-chart");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setBarGap(2);
        chart.setCategoryGap(14);
        chart.setVerticalGridLinesVisible(false);
        chart.setHorizontalZeroLineVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setAlternativeColumnFillVisible(false);

        thresholdLine.getStyleClass().add("chart-threshold-line");
        thresholdCaption.getStyleClass().add("chart-threshold-caption");

        overlay.setMouseTransparent(true);
        overlay.getChildren().addAll(thresholdLine, thresholdCaption);

        getChildren().addAll(chart, overlay);
    }

    /**
     * Baut das Diagramm aus dem aktuellen Datenstand neu auf.
     *
     * <p>Wird bei jeder Datenänderung aufgerufen; der Aufbau ist bewusst
     * vollständig, weil ein Notenbuch klein ist und der Code so keine
     * Teilaktualisierungen synchron halten muss.</p>
     *
     * @param semesters alle Semester in der Reihenfolge, in der sie erscheinen sollen
     */
    public void setData(List<Semester> semesters) {
        groups.clear();
        barStyleClasses.clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Integer> usedNames = new LinkedHashMap<>();
        List<String> categories = new ArrayList<>();

        for (Semester semester : semesters) {
            List<CourseModule> graded = semester.getModules().stream()
                    .filter(CourseModule::hasGrade)
                    .toList();
            if (graded.isEmpty()) {
                continue;
            }
            int firstIndex = categories.size();
            for (CourseModule module : graded) {
                String category = uniqueCategory(module.getName(), usedNames);
                categories.add(category);
                series.getData().add(createDataPoint(category, semester, module));
            }
            groups.add(new SemesterGroup(semester.getName(), firstIndex, categories.size() - 1));
        }

        xAxis.getCategories().clear();
        xAxis.setCategories(javafx.collections.FXCollections.observableArrayList(categories));
        chart.getData().setAll(List.of(series));

        syncOverlayNodes();
        requestLayout();
    }

    /** {@code true}, wenn mindestens eine Säule darstellbar ist. */
    public boolean hasData() {
        return !groups.isEmpty();
    }

    private XYChart.Data<String, Number> createDataPoint(String category, Semester semester,
                                                         CourseModule module) {
        BigDecimal rounded = module.getRoundedGrade();
        XYChart.Data<String, Number> point = new XYChart.Data<>(category, rounded);
        barStyleClasses.put(point, Formats.gradeColorClass("bar--", rounded));

        point.nodeProperty().addListener((observable, oldNode, node) -> {
            if (node == null) {
                return;
            }
            Tooltip tooltip = new Tooltip(
                    module.getName() + "\n"
                            + semester.getName() + "\n"
                            + "Modulnote " + Formats.grade(rounded)
                            + "  (" + Formats.exactLabel(module.getExactAverage()) + ")\n"
                            + Formats.statusText(rounded));
            tooltip.setShowDelay(Duration.millis(150));
            Tooltip.install(node, tooltip);

            node.setAccessibleText(module.getName() + ", Modulnote " + Formats.grade(rounded));
        });
        return point;
    }

    /**
     * Macht die Kategorie eindeutig, ohne das Etikett zu verändern: gleichnamige
     * Module in verschiedenen Semestern erhalten unsichtbare Zeichen angehängt.
     */
    private static String uniqueCategory(String name, Map<String, Integer> usedNames) {
        int count = usedNames.merge(name, 1, Integer::sum);
        if (count == 1) {
            return name;
        }
        return name + String.valueOf(INVISIBLE_MARKER).repeat(count - 1);
    }

    /** Legt für jede Semestergruppe die Overlay-Knoten an bzw. entfernt überzählige. */
    private void syncOverlayNodes() {
        overlay.getChildren().removeAll(dividers);
        overlay.getChildren().removeAll(semesterLabels);
        dividers.clear();
        semesterLabels.clear();

        for (int i = 0; i < groups.size(); i++) {
            Text label = new Text(groups.get(i).name());
            label.getStyleClass().add("chart-semester-label");
            semesterLabels.add(label);

            // Zwischen zwei Gruppen je ein Trenner.
            if (i > 0) {
                Line divider = new Line();
                divider.getStyleClass().add("chart-semester-divider");
                dividers.add(divider);
            }
        }
        overlay.getChildren().addAll(dividers);
        overlay.getChildren().addAll(semesterLabels);
    }

    @Override
    protected void layoutChildren() {
        limitBarWidth();
        super.layoutChildren();
        applyBarColours();
        positionOverlay();
    }

    /**
     * Hält die Säulen auf einer vernünftigen Breite, indem der Abstand zwischen
     * den Kategorien mitwächst. Bei wenigen Modulen wären die Säulen sonst
     * breiter als hoch.
     */
    private void limitBarWidth() {
        int count = xAxis.getCategories().size();
        double plotWidth = xAxis.getWidth();
        if (count == 0 || plotWidth <= 0) {
            return;
        }
        double categorySize = plotWidth / count;
        double gap = Math.max(MIN_CATEGORY_GAP, categorySize - MAX_BAR_WIDTH);
        if (Math.abs(chart.getCategoryGap() - gap) > 1) {
            chart.setCategoryGap(gap);
        }
    }

    /**
     * Setzt die Notenfarbe der Säulen.
     *
     * <p>{@code BarChart} überschreibt die Stilklassen seiner Säulen beim Aufbau,
     * deshalb wird die Farbe hier nach jedem Layoutdurchlauf nachgezogen. Die
     * Prüfung auf {@code contains} hält das günstig.</p>
     */
    private void applyBarColours() {
        barStyleClasses.forEach((point, styleClass) -> {
            Node node = point.getNode();
            if (node != null && !node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
        });
    }

    /**
     * Positioniert Grenzlinie, Semestertrenner und Semesterbeschriftungen anhand
     * der tatsächlichen Achsenpositionen.
     */
    private void positionOverlay() {
        Node plotBackground = chart.lookup(".chart-plot-background");
        if (plotBackground == null || groups.isEmpty()) {
            overlay.setVisible(false);
            return;
        }
        overlay.setVisible(true);

        Bounds plot = overlay.sceneToLocal(
                plotBackground.localToScene(plotBackground.getBoundsInLocal()));
        double left = plot.getMinX();
        double right = plot.getMaxX();
        double top = plot.getMinY();
        double bottom = plot.getMaxY();

        double thresholdY = top + yAxis.getDisplayPosition(GradeScale.PASSING.doubleValue());
        thresholdLine.setStartX(left);
        thresholdLine.setEndX(right);
        thresholdLine.setStartY(thresholdY);
        thresholdLine.setEndY(thresholdY);

        thresholdCaption.setX(right - thresholdCaption.getLayoutBounds().getWidth() - 6);
        thresholdCaption.setY(thresholdY - 5);

        int dividerIndex = 0;
        for (int i = 0; i < groups.size(); i++) {
            SemesterGroup group = groups.get(i);
            double groupStart = left + categoryCenter(group.firstIndex())
                    - categoryWidth() / 2.0;
            double groupEnd = left + categoryCenter(group.lastIndex()) + categoryWidth() / 2.0;

            Text label = semesterLabels.get(i);
            double labelWidth = label.getLayoutBounds().getWidth();
            double center = (groupStart + groupEnd) / 2.0;
            label.setX(Math.max(left + 2, Math.min(right - labelWidth - 2, center - labelWidth / 2.0)));
            label.setY(top + 14);

            if (i > 0 && dividerIndex < dividers.size()) {
                Line divider = dividers.get(dividerIndex++);
                double x = groupStart - chart.getCategoryGap() / 2.0;
                divider.setStartX(x);
                divider.setEndX(x);
                divider.setStartY(top);
                divider.setEndY(bottom);
            }
        }
    }

    private double categoryCenter(int index) {
        List<String> categories = xAxis.getCategories();
        if (index < 0 || index >= categories.size()) {
            return 0;
        }
        return xAxis.getDisplayPosition(categories.get(index));
    }

    private double categoryWidth() {
        List<String> categories = xAxis.getCategories();
        if (categories.size() < 2) {
            return xAxis.getWidth();
        }
        return Math.abs(categoryCenter(1) - categoryCenter(0));
    }

    /** Ein Semesterblock innerhalb der Kategorienreihe. */
    private record SemesterGroup(String name, int firstIndex, int lastIndex) {
    }
}
