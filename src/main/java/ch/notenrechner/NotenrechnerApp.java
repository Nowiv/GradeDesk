package ch.notenrechner;

import ch.notenrechner.model.GradeBook;
import ch.notenrechner.persistence.GradeRepository;
import ch.notenrechner.persistence.JsonGradeRepository;
import ch.notenrechner.persistence.LoadResult;
import ch.notenrechner.service.AutoSaveService;
import ch.notenrechner.service.GradeBookService;
import ch.notenrechner.ui.AppServices;
import ch.notenrechner.ui.MainController;
import ch.notenrechner.ui.control.Dialogs;
import ch.notenrechner.ui.util.Theme;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Einstiegspunkt des Notenrechners.
 *
 * <p>Hier werden die Bausteine einmal zusammengesetzt: Modell, Repository,
 * Service und automatisches Speichern. Die Anwendung kommt ohne
 * Abhängigkeitsinjektions-Framework aus – bei dieser Grösse ist eine
 * nachvollziehbare Verdrahtung an einer Stelle die bessere Wahl.</p>
 */
public final class NotenrechnerApp extends Application {

    private static final String MAIN_VIEW = "/ch/notenrechner/ui/main-view.fxml";
    private static final String APP_ICON = "/ch/notenrechner/icon/notenrechner-256.png";

    private AutoSaveService autoSave;

    @Override
    public void start(Stage stage) throws IOException {
        GradeRepository repository = new JsonGradeRepository();
        GradeBookService service = new GradeBookService(new GradeBook(), repository);

        LoadResult loadResult = service.load();

        // Erst nach dem Laden starten, damit das Laden selbst kein Speichern auslöst.
        autoSave = new AutoSaveService(service, Platform::runLater);
        autoSave.start();

        FXMLLoader loader = new FXMLLoader(resource(MAIN_VIEW));
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.init(new AppServices(service, autoSave));

        Scene scene = new Scene(root, 1180, 760);
        Theme.apply(scene);

        stage.setScene(scene);
        stage.setTitle("Notenrechner");
        stage.setMinWidth(1060);
        stage.setMinHeight(640);
        stage.getIcons().add(new javafx.scene.image.Image(
                Objects.requireNonNull(getClass().getResourceAsStream(APP_ICON),
                        "Programmsymbol fehlt im Paket: " + APP_ICON)));
        stage.show();

        if (loadResult.needsAttention()) {
            Platform.runLater(() -> Dialogs.info(stage, "Hinweis zu den Daten", loadResult.message()));
        }
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
        return Objects.requireNonNull(NotenrechnerApp.class.getResource(path),
                "Ressource fehlt im Paket: " + path);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
