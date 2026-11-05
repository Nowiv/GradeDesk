package ch.notenrechner.ui;

import ch.notenrechner.service.AutoSaveService;
import ch.notenrechner.ui.util.Icons;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Steuert das Grundgerüst: Seitenleiste, Ansichtswechsel und Speicheranzeige.
 *
 * <p>Die Navigationseinträge stammen aus {@link Views}. Ansichten werden beim
 * ersten Aufruf geladen und danach wiederverwendet, damit ein Wechsel ohne
 * Verzögerung und ohne Verlust des Zustands erfolgt.</p>
 */
public final class MainController {

    /** Der eine gestaltete Moment der Anwendung: der Wechsel der Ansicht. */
    private static final Duration VIEW_TRANSITION = Duration.millis(170);

    @FXML
    private VBox navigationBox;
    @FXML
    private StackPane contentArea;
    @FXML
    private Label saveStatusLabel;
    @FXML
    private Label locationLabel;
    @FXML
    private HBox alertBanner;
    @FXML
    private Label alertBannerLabel;

    private final Map<String, LoadedView> views = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    private AppServices services;
    private String activeId;

    /** Baut die Seitenleiste und zeigt die erste Ansicht. */
    public void init(AppServices services) {
        this.services = services;

        List<NavigationEntry> entries = Views.entries();
        for (NavigationEntry entry : entries) {
            Button button = createNavButton(entry);
            navButtons.put(entry.id(), button);
            navigationBox.getChildren().add(button);
        }

        // In der Seitenleiste nur der Hinweis, der vollstaendige Pfad als Kurzinfo
        // und in den Einstellungen – sonst zerreisst ein langer Pfad die Spalte.
        locationLabel.setText("Daten im Benutzerprofil");
        locationLabel.setTooltip(new Tooltip(services.gradeBook().describeLocation()));
        bindSaveStatus(services.autoSave());

        alertBanner.getChildren().add(0, Icons.create(Icons.ALERT, 18));

        if (!entries.isEmpty()) {
            navigateTo(entries.get(0).id());
        }
    }

    private Button createNavButton(NavigationEntry entry) {
        Button button = new Button(entry.title());
        button.getStyleClass().add("nav-button");
        button.setGraphic(Icons.create(entry.iconPath(), 19, "nav-button__icon"));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> navigateTo(entry.id()));
        button.setAccessibleText(entry.title());
        return button;
    }

    /**
     * Wechselt zur Ansicht mit dieser Kennung.
     *
     * @param id eine Kennung aus {@link Views#entries()}
     */
    public void navigateTo(String id) {
        if (id.equals(activeId)) {
            return;
        }
        LoadedView view = views.computeIfAbsent(id, this::loadView);

        contentArea.getChildren().setAll(view.root());
        animateEntrance(view.root());
        view.controller().onShown();

        activeId = id;
        navButtons.forEach((buttonId, button) -> {
            boolean active = buttonId.equals(id);
            button.getStyleClass().remove("nav-button--active");
            if (active) {
                button.getStyleClass().add("nav-button--active");
            }
        });
    }

    private LoadedView loadView(String id) {
        NavigationEntry entry = Views.entries().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unbekannte Ansicht: " + id));
        try {
            FXMLLoader loader = new FXMLLoader(requireResource(entry.fxml()));
            ViewController controller = entry.controllerFactory().get();
            loader.setController(controller);
            Node root = loader.load();
            controller.init(services);
            return new LoadedView(root, controller);
        } catch (IOException e) {
            throw new UncheckedIOException("Die Ansicht " + entry.fxml() + " konnte nicht geladen werden.", e);
        }
    }

    private static java.net.URL requireResource(String path) {
        java.net.URL url = MainController.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("FXML-Datei fehlt im Paket: " + path);
        }
        return url;
    }

    /** Kurzes Einblenden mit leichter Aufwärtsbewegung – Zustandswechsel, keine Dekoration. */
    private void animateEntrance(Node root) {
        FadeTransition fade = new FadeTransition(VIEW_TRANSITION, root);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition rise = new TranslateTransition(VIEW_TRANSITION, root);
        rise.setFromY(8);
        rise.setToY(0);

        ParallelTransition transition = new ParallelTransition(fade, rise);
        transition.setInterpolator(Interpolator.SPLINE(0.16, 1, 0.3, 1));
        transition.play();
    }

    private void bindSaveStatus(AutoSaveService autoSave) {
        Region dot = new Region();
        dot.getStyleClass().add("save-status__dot");
        saveStatusLabel.setGraphic(dot);

        updateSaveStatus(autoSave, dot);
        autoSave.stateProperty().addListener(
                (observable, oldState, newState) -> updateSaveStatus(autoSave, dot));
    }

    private void updateSaveStatus(AutoSaveService autoSave, Region dot) {
        dot.getStyleClass().removeAll(
                "save-status__dot--saved", "save-status__dot--pending", "save-status__dot--failed");

        switch (autoSave.getState()) {
            case IDLE -> {
                saveStatusLabel.setText("Automatisch gespeichert");
                hideBanner();
            }
            case PENDING -> {
                dot.getStyleClass().add("save-status__dot--pending");
                saveStatusLabel.setText("Wird gespeichert …");
            }
            case SAVED -> {
                dot.getStyleClass().add("save-status__dot--saved");
                saveStatusLabel.setText("Gespeichert");
                hideBanner();
            }
            case FAILED -> {
                dot.getStyleClass().add("save-status__dot--failed");
                saveStatusLabel.setText("Speichern fehlgeschlagen");
                showBanner("Die letzte Änderung konnte nicht gespeichert werden. "
                        + autoSave.lastErrorProperty().get()
                        + " Die Noten bleiben im Programm erhalten, bis das Speichern wieder gelingt.");
            }
        }
    }

    /** Zeigt eine Meldung über der Arbeitsfläche, etwa nach einem Ladeproblem. */
    public void showBanner(String message) {
        alertBannerLabel.setText(message);
        alertBanner.setVisible(true);
        alertBanner.setManaged(true);
    }

    private void hideBanner() {
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);
    }

    /** Eine geladene Ansicht mit ihrem Controller. */
    private record LoadedView(Node root, ViewController controller) {
    }
}
