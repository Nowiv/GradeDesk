package ch.notenrechner.ui.view;

import ch.notenrechner.model.GradeBook;
import ch.notenrechner.ui.AppServices;
import ch.notenrechner.ui.ViewController;
import ch.notenrechner.ui.chart.GradeBarChart;
import ch.notenrechner.ui.util.Icons;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Die Diagrammansicht.
 *
 * <p>Enthält neben dem Diagramm eine Legende der Notenfarben. Die Legende ist
 * kein Schmuck: sie macht die Abstufung des Grüns lesbar, ohne dass jemand
 * Farbtöne vergleichen muss.</p>
 */
public final class ChartController implements ViewController {

    private static final List<LegendItem> LEGEND = List.of(
            new LegendItem("unter 4.0", "grade-fill--insufficient"),
            new LegendItem("4.0", "grade-fill--40"),
            new LegendItem("4.5", "grade-fill--45"),
            new LegendItem("5.0", "grade-fill--50"),
            new LegendItem("5.5", "grade-fill--55"),
            new LegendItem("6.0", "grade-fill--60"));

    @FXML
    private StackPane chartHolder;
    @FXML
    private HBox legendBox;

    private final GradeBarChart chart = new GradeBarChart();
    private Node emptyState;
    private GradeBook gradeBook;

    @Override
    public void init(AppServices services) {
        this.gradeBook = services.gradeBook().getGradeBook();

        emptyState = createEmptyState();
        chartHolder.getChildren().addAll(chart, emptyState);
        buildLegend();

        gradeBook.getSemesters().addListener((InvalidationListener) observable -> refresh());
        refresh();
    }

    @Override
    public void onShown() {
        refresh();
    }

    private void refresh() {
        chart.setData(gradeBook.getSemesters());
        boolean hasData = chart.hasData();
        chart.setVisible(hasData);
        emptyState.setVisible(!hasData);
        legendBox.setVisible(hasData);
        legendBox.setManaged(hasData);
    }

    private void buildLegend() {
        legendBox.setAlignment(Pos.CENTER_LEFT);
        legendBox.setSpacing(18);

        Label caption = new Label("MODULNOTE");
        caption.getStyleClass().add("label-micro");
        legendBox.getChildren().add(caption);

        for (LegendItem item : LEGEND) {
            Region swatch = new Region();
            swatch.getStyleClass().addAll("chart-legend-swatch", item.styleClass());

            Label label = new Label(item.label());
            label.getStyleClass().add("chart-legend-label");

            HBox entry = new HBox(swatch, label);
            entry.setSpacing(7);
            entry.setAlignment(Pos.CENTER_LEFT);
            legendBox.getChildren().add(entry);
        }
    }

    private Node createEmptyState() {
        HBox mark = new HBox(Icons.create(Icons.CHART, 32));
        mark.getStyleClass().add("empty-state__mark");
        mark.setAlignment(Pos.CENTER);

        Label title = new Label("Noch nichts zu zeigen");
        title.getStyleClass().add("empty-state__title");

        Label text = new Label("Sobald ein Modul mindestens eine Note hat, erscheint es hier "
                + "als Säule. Module ohne Noten bleiben aussen vor.");
        text.getStyleClass().add("empty-state__text");

        VBox box = new VBox(mark, title, text);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** Ein Eintrag der Farblegende. */
    private record LegendItem(String label, String styleClass) {
    }
}
