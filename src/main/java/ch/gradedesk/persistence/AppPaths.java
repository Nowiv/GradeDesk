package ch.gradedesk.persistence;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Ermittelt das Verzeichnis, in dem die Anwendung ihre Daten ablegt.
 *
 * <p>Die Daten liegen bewusst im Benutzerprofil und nicht im Installationsordner.
 * Dadurch bleiben sie bei Updates und Neuinstallationen erhalten, jeder
 * Betriebssystem-Benutzer hat automatisch seine eigenen Noten, und die
 * Anwendung braucht keine Schreibrechte im Programmverzeichnis.</p>
 *
 * <ul>
 *   <li>Windows: {@code %APPDATA%\GradeDesk\}</li>
 *   <li>macOS: {@code ~/Library/Application Support/GradeDesk/}</li>
 *   <li>Linux: {@code ~/.local/share/gradedesk/} (bzw. {@code $XDG_DATA_HOME})</li>
 * </ul>
 */
public final class AppPaths {

    /** Verzeichnisname unter Windows und macOS. */
    public static final String APP_DIR_NAME = "GradeDesk";

    /** Verzeichnisname unter Linux (Kleinschreibung ist dort üblich). */
    public static final String APP_DIR_NAME_LINUX = "gradedesk";

    private AppPaths() {
    }

    /** Das Datenverzeichnis für das aktuelle Betriebssystem. */
    public static Path dataDirectory() {
        return dataDirectory(
                System.getProperty("os.name", ""),
                Paths.get(System.getProperty("user.home", ".")),
                System.getenv("APPDATA"),
                System.getenv("XDG_DATA_HOME"));
    }

    /**
     * Wie {@link #dataDirectory()}, aber mit explizit übergebener Umgebung.
     * Diese Variante macht die Pfadlogik ohne echtes Betriebssystem testbar.
     *
     * @param osName       Wert von {@code os.name}
     * @param userHome     Heimatverzeichnis des Benutzers
     * @param appData      Wert von {@code %APPDATA%}, darf {@code null} sein
     * @param xdgDataHome  Wert von {@code $XDG_DATA_HOME}, darf {@code null} sein
     */
    public static Path dataDirectory(String osName, Path userHome, String appData, String xdgDataHome) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            Path base = isBlank(appData)
                    ? userHome.resolve("AppData").resolve("Roaming")
                    : Paths.get(appData);
            return base.resolve(APP_DIR_NAME);
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return userHome.resolve("Library").resolve("Application Support").resolve(APP_DIR_NAME);
        }
        Path base = isBlank(xdgDataHome)
                ? userHome.resolve(".local").resolve("share")
                : Paths.get(xdgDataHome);
        return base.resolve(APP_DIR_NAME_LINUX);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
