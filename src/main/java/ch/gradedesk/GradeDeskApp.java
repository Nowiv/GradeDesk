package ch.gradedesk;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.i18n.Language;
import ch.gradedesk.ui.util.Appearance;
import ch.gradedesk.model.GradeBook;
import ch.gradedesk.persistence.GradeRepository;
import ch.gradedesk.persistence.JsonGradeRepository;
import ch.gradedesk.persistence.LoadResult;
import ch.gradedesk.persistence.SettingsStore;
import ch.gradedesk.service.AutoSaveService;
import ch.gradedesk.service.GradeBookService;
import ch.gradedesk.ui.AppServices;
import ch.gradedesk.ui.MainController;
import ch.gradedesk.ui.control.Dialogs;
import ch.gradedesk.ui.util.Theme;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * Einstiegspunkt von GradeDesk.
 *
 * <p>Hier werden die Bausteine einmal zusammengesetzt: Spracheinstellung,
 * Modell, Repository, Service und automatisches Speichern. Die Anwendung kommt
 * ohne Abhängigkeitsinjektions-Framework aus – bei dieser Grösse ist eine
 * nachvollziehbare Verdrahtung an einer Stelle die bessere Wahl.</p>
 */
public final class GradeDeskApp extends Application {

    private static final String MAIN_VIEW = "/ch/gradedesk/ui/main-view.fxml";
    private static final String APP_ICON = "/ch/gradedesk/icon/gradedesk-256.png";

    private AutoSaveService autoSave;

    @Override
    public void start(Stage stage) throws IOException {
        // Sprache zuerst: alles Folgende liest bereits übersetzte Texte.
        SettingsStore settings = new SettingsStore();
        restorePreferences(settings);

        GradeRepository repository = new JsonGradeRepository();
        GradeBookService service = new GradeBookService(new GradeBook(), repository);

        LoadResult loadResult = service.load();

        // Erst nach dem Laden starten, damit das Laden selbst kein Speichern auslöst.
        autoSave = new AutoSaveService(service, Platform::runLater);
        autoSave.start();

        FXMLLoader loader = new FXMLLoader(resource(MAIN_VIEW), I18n.bundle());
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.init(new AppServices(service, autoSave));

        Scene scene = new Scene(root, 1240, 800);
        Theme.apply(scene);

        stage.setScene(scene);
        stage.setTitle(I18n.t("app.name"));
        stage.setMinWidth(1060);
        stage.setMinHeight(660);
        stage.getIcons().add(new Image(icon()));
        stage.show();

        if (loadResult.needsAttention()) {
            Platform.runLater(() -> Dialogs.info(stage,
                    I18n.t("dialog.dataNotice.title"), describe(loadResult)));
        }
    }

    /**
     * Stellt die zuletzt gewählte Sprache wieder her und merkt sich jede weitere
     * Wahl. Ohne gespeicherte Einstellung entscheidet die Systemsprache.
     */
    private void restorePreferences(SettingsStore settings) {
        Optional<SettingsStore.AppSettings> stored = settings.load();

        stored.map(SettingsStore.AppSettings::language)
                .flatMap(Language::fromTag)
                .ifPresent(I18n::setLanguage);

        stored.map(SettingsStore.AppSettings::appearance)
                .flatMap(Appearance::fromTag)
                .ifPresent(Theme::setAppearance);

        I18n.languageProperty().addListener((observable, previous, current) ->
                settings.save(SettingsStore.AppSettings.of(
                        current.tag(), Theme.getAppearance().tag())));

        Theme.appearanceProperty().addListener((observable, previous, current) ->
                settings.save(SettingsStore.AppSettings.of(
                        I18n.getLanguage().tag(), current.tag())));
    }

    /** Formuliert das Ladeergebnis in der eingestellten Sprache. */
    private static String describe(LoadResult result) {
        String message = switch (result.status()) {
            case RECOVERED_FROM_BROKEN_FILE -> I18n.t("load.brokenFile");
            case RECOVERED_FROM_MISSING_FILE -> I18n.t("load.missingFile");
            case CORRUPTED -> I18n.t("load.corrupted");
            default -> "";
        };
        if (result.detail() != null && !result.detail().isBlank()) {
            message = message + " " + I18n.t("load.quarantineHint", result.detail());
        }
        return message;
    }

    @Override
    public void stop() {
        if (autoSave != null) {
            // Eine noch nicht geschriebene Änderung darf beim Schliessen nicht verloren gehen.
            autoSave.flush();
            autoSave.close();
        }
    }

    private static URL resource(String path) {
        return Objects.requireNonNull(GradeDeskApp.class.getResource(path),
                "Missing resource: " + path);
    }

    private static InputStream icon() {
        return Objects.requireNonNull(GradeDeskApp.class.getResourceAsStream(APP_ICON),
                "Missing resource: " + APP_ICON);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
