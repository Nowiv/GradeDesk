package ch.gradedesk.update;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.ui.util.Theme;
import ch.gradedesk.persistence.SettingsStore;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** JavaFX presentation and platform-specific hand-off for updater operations. */
public final class UpdateUi {
    private final UpdateService service;
    private final SettingsStore settings;
    private final Runnable saveBeforeExit;

    /**
     * @param saveBeforeExit writes pending changes to disk; called right before
     *                       the application exits for a Windows update
     */
    public UpdateUi(UpdateService service, SettingsStore settings, Runnable saveBeforeExit) {
        this.service = service;
        this.settings = settings;
        this.saveBeforeExit = saveBeforeExit;
    }

    public void check(Window owner, boolean automatic) {
        if (automatic && !settings.load().map(SettingsStore.AppSettings::automaticUpdatesEnabled).orElse(true)) return;
        Task<UpdateService.CheckResult> task = new Task<>() {
            @Override protected UpdateService.CheckResult call() throws Exception { return service.check(); }
        };
        task.setOnSucceeded(event -> {
            UpdateService.CheckResult result = task.getValue();
            if (result.available()) {
                String skipped = settings.load().map(SettingsStore.AppSettings::skippedVersion).orElse(null);
                if (automatic && result.latest().toString().equals(skipped)) return;
                showUpdate(owner, result);
            } else if (!automatic) {
                String message = switch (result.status()) {
                    case IDE_LAUNCH -> I18n.t("update.ide");
                    case RATE_LIMITED -> I18n.t("update.rateLimited");
                    case NO_RELEASE -> I18n.t("update.noRelease");
                    case UP_TO_DATE -> I18n.t("update.current", result.current());
                    default -> I18n.t("update.failed");
                };
                info(owner, I18n.t("update.title"), message);
            }
        });
        task.setOnFailed(event -> {
            if (!automatic) info(owner, I18n.t("update.title"), I18n.t("update.error", safeMessage(task.getException())));
        });
        Thread thread = new Thread(task, "gradedesk-update-check");
        thread.setDaemon(true);
        thread.start();
    }

    private void showUpdate(Window owner, UpdateService.CheckResult update) {
        ButtonType install = new ButtonType(I18n.t("update.install"), ButtonBar.ButtonData.YES);
        ButtonType later = new ButtonType(I18n.t("update.later"), ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType skip = new ButtonType(I18n.t("update.skip"), ButtonBar.ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(I18n.t("update.title"));
        alert.setHeaderText(I18n.t("update.available", update.latest()));
        String notes = update.releaseNotes() == null || update.releaseNotes().isBlank()
                ? I18n.t("update.noNotes") : update.releaseNotes();
        Label notesLabel = new Label(notes);
        notesLabel.setWrapText(true);
        notesLabel.setMaxWidth(560);
        alert.getDialogPane().setContent(notesLabel);
        alert.getButtonTypes().setAll(install, later, skip);
        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() == later) return;
        if (choice.get() == skip) {
            saveSkipped(update.latest().toString());
            return;
        }
        download(owner, update);
    }

    private void download(Window owner, UpdateService.CheckResult update) {
        Dialog<Void> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(I18n.t("update.downloading"));
        ProgressBar progress = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        Label status = new Label(I18n.t("update.downloading"));
        VBox box = new VBox(12, status, progress);
        box.setPrefWidth(390);
        dialog.getDialogPane().setContent(box);
        dialog.setOnCloseRequest(event -> event.consume());
        Task<Path> task = new Task<>() {
            @Override protected Path call() throws Exception {
                return service.downloadAndVerify(update, value -> {
                    if (value >= 0) updateProgress(value, 100);
                });
            }
        };
        progress.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(event -> {
            dialog.close();
            try { handOff(owner, task.getValue()); }
            catch (Exception e) { info(owner, I18n.t("update.title"), I18n.t("update.installError", safeMessage(e))); }
        });
        task.setOnFailed(event -> {
            dialog.close();
            info(owner, I18n.t("update.title"), I18n.t("update.downloadError", safeMessage(task.getException())));
        });
        Thread thread = new Thread(task, "gradedesk-update-download");
        thread.setDaemon(true);
        thread.start();
        dialog.show();
    }

    private void handOff(Window owner, Path installer) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            // Start the helper first. If it cannot run, the application has to
            // stay alive and report the failure instead of exiting into a
            // half-finished update.
            WindowsInstallerHandoff.start(installer,
                    System.getProperty("jpackage.app-path"),
                    WindowsInstallerHandoff.logFile());

            // The helper is now waiting for this process, so anything unsaved
            // has to reach the disk before it goes away.
            saveBeforeExit.run();

            // Platform.exit() on its own leaves the JVM running as long as any
            // non-daemon thread exists, and a live GradeDesk.exe is exactly what
            // blocks the MSI. System.exit makes the exit unconditional.
            Platform.exit();
            System.exit(0);
            return;
        }
        if (os.contains("mac") || os.contains("darwin")) {
            new ProcessBuilder("open", installer.toAbsolutePath().toString()).start();
            info(owner, I18n.t("update.title"), I18n.t("update.macInstructions"));
            return;
        }
        Path folder = installer.toAbsolutePath().getParent();
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(folder.toFile());
        info(owner, I18n.t("update.title"), I18n.t("update.linuxInstructions", installer.getFileName()));
    }

    private void saveSkipped(String version) {
        SettingsStore.AppSettings old = settings.load().orElse(SettingsStore.AppSettings.of(I18n.getLanguage().tag(), Theme.getAppearance().tag()));
        settings.save(old.withSkippedVersion(version));
    }

    private static void info(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static String safeMessage(Throwable throwable) {
        return throwable == null || throwable.getMessage() == null ? "Unknown error" : throwable.getMessage();
    }
}
