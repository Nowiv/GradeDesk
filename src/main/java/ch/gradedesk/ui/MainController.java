package ch.gradedesk.ui;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.service.AutoSaveService;
import ch.gradedesk.ui.control.LanguageSwitch;
import ch.gradedesk.ui.util.Icons;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Steuert das Grundgerüst: Seitenleiste, Ansichtswechsel, Sprache und Speicheranzeige.
 *
 * <p>Die Navigationseinträge stammen aus {@link Views}. Ansichten werden beim
 * ersten Aufruf geladen und danach wiederverwendet, damit ein Wechsel ohne
 * Verzögerung und ohne Verlust des Zustands erfolgt.</p>
 *
 * <p>Beim Sprachwechsel werden die zwischengespeicherten Ansichten verworfen und
 * mit dem neuen Textbündel neu geladen. Das ist einfacher und verlässlicher, als
 * jede einzelne Beschriftung an eine Property zu binden – und es erwischt auch
 * die Texte, die erst zur Laufzeit entstehen.</p>
 */
public final class MainController {

    /** Der eine gestaltete Moment der Anwendung: der Wechsel der Ansicht. */
    private static final Duration VIEW_TRANSITION = Duration.millis(170);

    @FXML
    private Menu helpMenu;
    @FXML
    private MenuItem checkUpdatesMenuItem;
    @FXML
    private Label wordmarkPrefixLabel;
    @FXML
    private Label wordmarkSuffixLabel;
    @FXML
    private Label taglineLabel;
    @FXML
    private VBox navigationBox;
    @FXML
    private VBox sidebarFooter;
    @FXML
    private StackPane contentArea;
    @FXML
    private Label saveStatusLabel;
    @FXML
    private Label locationLabel;
    @FXML
    private Label languageLabel;
    @FXML
    private HBox alertBanner;
    @FXML
    private Label alertBannerLabel;

    private final Map<String, LoadedView> views = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final Region saveStatusDot = new Region();

    private AppServices services;
    private String activeId;

    /** Baut die Seitenleiste und zeigt die erste Ansicht. */
    public void init(AppServices services) {
        this.services = services;

        buildNavigation();

        saveStatusDot.getStyleClass().add("save-status__dot");
        saveStatusLabel.setGraphic(saveStatusDot);
        bindSaveStatus(services.autoSave());

        alertBanner.getChildren().add(0, Icons.create(Icons.ALERT, 18));

        sidebarFooter.getChildren().add(new LanguageSwitch());

        applyTexts();
        I18n.languageProperty().addListener((observable, previous, current) -> onLanguageChanged());

        List<NavigationEntry> entries = Views.entries();
        if (!entries.isEmpty()) {
            navigateTo(entries.get(0).id());
        }
    }

    private void buildNavigation() {
        navigationBox.getChildren().clear();
        navButtons.clear();
        for (NavigationEntry entry : Views.entries()) {
            Button button = createNavButton(entry);
            navButtons.put(entry.id(), button);
            navigationBox.getChildren().add(button);
        }
    }

    private Button createNavButton(NavigationEntry entry) {
        Button button = new Button(I18n.t(entry.titleKey()));
        button.getStyleClass().add("nav-button");
        button.setGraphic(Icons.create(entry.iconPath(), 19, "nav-button__icon"));
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> navigateTo(entry.id()));
        button.setAccessibleText(I18n.t(entry.titleKey()));
        return button;
    }

    /** Setzt alle Texte, die nicht aus einer FXML-Datei stammen. */
    private void applyTexts() {
        // Die Wortmarke steht zweifarbig, aber nicht als Farbverlauf: zwei Labels,
        // damit der Name auch bei hohem Kontrast lesbar bleibt.
        helpMenu.setText(I18n.t("menu.help"));
        checkUpdatesMenuItem.setText(I18n.t("menu.checkUpdates"));
        wordmarkPrefixLabel.setText("Grade");
        wordmarkSuffixLabel.setText("Desk");
        taglineLabel.setText(I18n.t("app.tagline"));
        languageLabel.setText(I18n.t("sidebar.language"));

        locationLabel.setText(I18n.t("sidebar.dataLocation"));
        locationLabel.setTooltip(new Tooltip(services.gradeBook().describeLocation()));

        updateSaveStatus(services.autoSave());
    }

    /** Lädt alle Ansichten mit dem neuen Textbündel neu. */
    private void onLanguageChanged() {
        String current = activeId;
        views.clear();
        buildNavigation();
        applyTexts();
        activeId = null;
        navigateTo(current == null ? Views.entries().get(0).id() : current);
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
            button.getStyleClass().remove("nav-button--active");
            if (buttonId.equals(id)) {
                button.getStyleClass().add("nav-button--active");
            }
        });
    }

    private LoadedView loadView(String id) {
        NavigationEntry entry = Views.entries().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown view: " + id));
        try {
            FXMLLoader loader = new FXMLLoader(requireResource(entry.fxml()), I18n.bundle());
            ViewController controller = entry.controllerFactory().get();
            loader.setController(controller);
            Node root = loader.load();
            controller.init(services);
            return new LoadedView(root, controller);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load view " + entry.fxml(), e);
        }
    }

    private static URL requireResource(String path) {
        URL url = MainController.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Missing FXML resource: " + path);
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
        updateSaveStatus(autoSave);
        autoSave.stateProperty().addListener(
                (observable, oldState, newState) -> updateSaveStatus(autoSave));
    }

    private void updateSaveStatus(AutoSaveService autoSave) {
        saveStatusDot.getStyleClass().removeAll(
                "save-status__dot--saved", "save-status__dot--pending", "save-status__dot--failed");

        switch (autoSave.getState()) {
            case IDLE -> {
                saveStatusLabel.setText(I18n.t("save.idle"));
                hideBanner();
            }
            case PENDING -> {
                saveStatusDot.getStyleClass().add("save-status__dot--pending");
                saveStatusLabel.setText(I18n.t("save.pending"));
            }
            case SAVED -> {
                saveStatusDot.getStyleClass().add("save-status__dot--saved");
                saveStatusLabel.setText(I18n.t("save.saved"));
                hideBanner();
            }
            case FAILED -> {
                saveStatusDot.getStyleClass().add("save-status__dot--failed");
                saveStatusLabel.setText(I18n.t("save.failed"));
                showBanner(I18n.t("save.failed.banner", autoSave.lastErrorProperty().get()));
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


    /** Manuelle Suche aus dem Hilfe-Menü. */
    @FXML
    public void checkForUpdates() {
        checkForUpdates(false);
    }

    /** Startet die Suche ausserhalb des JavaFX-Threads. */
    public void checkForUpdates(boolean automatic) {
        services.updateUi().check(contentArea.getScene() == null ? null : contentArea.getScene().getWindow(), automatic);
    }

    /** Eine geladene Ansicht mit ihrem Controller. */
    private record LoadedView(Node root, ViewController controller) {
    }
}
