package ch.notenrechner.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppPathsTest {

    private static final Path HOME = Paths.get("/home/schule");

    @Test
    @DisplayName("Windows: Daten liegen unter %APPDATA%")
    void windowsUsesAppData() {
        Path dir = AppPaths.dataDirectory("Windows 11", HOME, "C:\\Users\\schule\\AppData\\Roaming", null);

        assertEquals(Paths.get("C:\\Users\\schule\\AppData\\Roaming", "Notenrechner"), dir);
    }

    @Test
    @DisplayName("Windows ohne %APPDATA%: Rückfall auf das Benutzerprofil")
    void windowsFallsBackToUserHome() {
        Path dir = AppPaths.dataDirectory("Windows 11", HOME, null, null);

        assertEquals(HOME.resolve("AppData").resolve("Roaming").resolve("Notenrechner"), dir);
    }

    @Test
    @DisplayName("macOS: Daten liegen unter Library/Application Support")
    void macUsesApplicationSupport() {
        Path dir = AppPaths.dataDirectory("Mac OS X", HOME, null, null);

        assertEquals(HOME.resolve("Library").resolve("Application Support").resolve("Notenrechner"), dir);
    }

    @Test
    @DisplayName("Linux: Daten liegen unter ~/.local/share")
    void linuxUsesLocalShare() {
        Path dir = AppPaths.dataDirectory("Linux", HOME, null, null);

        assertEquals(HOME.resolve(".local").resolve("share").resolve("notenrechner"), dir);
    }

    @Test
    @DisplayName("Linux: XDG_DATA_HOME wird respektiert")
    void linuxRespectsXdgDataHome() {
        Path dir = AppPaths.dataDirectory("Linux", HOME, null, "/home/schule/.daten");

        assertEquals(Paths.get("/home/schule/.daten", "notenrechner"), dir);
    }

    @Test
    @DisplayName("Die Daten liegen nie im Installationsverzeichnis")
    void neverUsesWorkingDirectory() {
        Path dir = AppPaths.dataDirectory("Linux", HOME, null, null);

        assertEquals(HOME, dir.getParent().getParent().getParent());
    }
}
