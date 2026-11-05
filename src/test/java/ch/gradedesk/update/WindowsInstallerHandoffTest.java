package ch.gradedesk.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsInstallerHandoffTest {

    /** A file name made of everything that usually breaks naive quoting. */
    private static final String HOSTILE_NAME = "O'Brien $(calc); Remove-Item -Recurse.msi";

    private static String decodedScript() {
        return new String(Base64.getDecoder().decode(WindowsInstallerHandoff.encodedScript()),
                StandardCharsets.UTF_16LE);
    }

    @Test void startsHiddenPowerShellWithoutProfile() {
        List<String> command = WindowsInstallerHandoff.command();

        assertEquals(List.of("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-WindowStyle", "Hidden", "-EncodedCommand"), command.subList(0, 7));
        assertEquals(8, command.size(), "only the encoded script may follow");
    }

    @Test void encodedScriptIsUtf16LeWithoutBom() {
        byte[] raw = Base64.getDecoder().decode(WindowsInstallerHandoff.encodedScript());

        assertTrue(raw.length > 2);
        assertFalse(raw[0] == (byte) 0xFF && raw[1] == (byte) 0xFE,
                "a BOM would make PowerShell reject the encoded command");
        assertTrue(decodedScript().startsWith("$ErrorActionPreference"));
    }

    @Test void scriptWaitsInstallsRestartsAndCleansUp() {
        String script = decodedScript();

        assertAll(
                () -> assertTrue(script.contains("Wait-Process -Id $target"), "waits for the app"),
                () -> assertTrue(script.contains("msiexec.exe"), "installs"),
                () -> assertTrue(script.contains("'/passive'"), "installs unattended"),
                () -> assertTrue(script.contains("'/norestart'"), "never reboots on its own"),
                () -> assertTrue(script.contains("'/l*v'"), "writes a verbose log"),
                () -> assertTrue(script.contains("-Wait -PassThru"), "collects the exit code"),
                () -> assertTrue(script.contains("$code -eq 0 -or $code -eq 3010"),
                        "treats 3010 as success"),
                () -> assertTrue(script.contains("Start-Process -FilePath $app"), "restarts"),
                () -> assertTrue(script.contains("Remove-Item -LiteralPath $msi"),
                        "removes the installer"));
    }

    @Test void scriptReadsEveryValueFromTheEnvironment() {
        String script = decodedScript();

        assertAll(
                () -> assertTrue(script.contains("$env:" + WindowsInstallerHandoff.ENV_PID)),
                () -> assertTrue(script.contains("$env:" + WindowsInstallerHandoff.ENV_MSI)),
                () -> assertTrue(script.contains("$env:" + WindowsInstallerHandoff.ENV_APP)),
                () -> assertTrue(script.contains("$env:" + WindowsInstallerHandoff.ENV_LOG)));
    }

    @Test void environmentCarriesPidInstallerAppAndLog(@TempDir Path tmp) {
        Path installer = tmp.resolve("GradeDesk-2.1.5.msi");
        Path log = tmp.resolve("update-install.log");

        Map<String, String> environment =
                WindowsInstallerHandoff.environment(installer, "C:\\Apps\\GradeDesk.exe", log);

        assertAll(
                () -> assertEquals(Long.toString(ProcessHandle.current().pid()),
                        environment.get(WindowsInstallerHandoff.ENV_PID)),
                () -> assertEquals(installer.toAbsolutePath().toString(),
                        environment.get(WindowsInstallerHandoff.ENV_MSI)),
                () -> assertEquals("C:\\Apps\\GradeDesk.exe",
                        environment.get(WindowsInstallerHandoff.ENV_APP)),
                () -> assertEquals(log.toAbsolutePath().toString(),
                        environment.get(WindowsInstallerHandoff.ENV_LOG)));
    }

    @Test void missingAppPathBecomesEmptySoTheScriptSkipsTheRestart(@TempDir Path tmp) {
        Map<String, String> environment = WindowsInstallerHandoff.environment(
                tmp.resolve("x.msi"), null, tmp.resolve("x.log"));

        assertEquals("", environment.get(WindowsInstallerHandoff.ENV_APP));
    }

    /**
     * The point of the whole design: a hostile path reaches the helper only as an
     * environment value, never as part of the command line or the script.
     */
    @Test void hostilePathsNeverReachTheCommandLineOrTheScript(@TempDir Path tmp) {
        Path installer = tmp.resolve(HOSTILE_NAME);
        String appPath = "C:\\Users\\O'Brien $(calc)\\GradeDesk.exe";

        List<String> command = WindowsInstallerHandoff.command();
        Map<String, String> environment =
                WindowsInstallerHandoff.environment(installer, appPath, tmp.resolve("i.log"));

        assertAll(
                () -> assertTrue(command.stream().noneMatch(part -> part.contains("O'Brien")),
                        "command line must not carry the path"),
                () -> assertTrue(command.stream().noneMatch(part -> part.contains("calc")),
                        "command line must not carry the payload"),
                () -> assertFalse(decodedScript().contains("O'Brien"),
                        "script is a constant and must not be templated"),
                () -> assertTrue(environment.get(WindowsInstallerHandoff.ENV_MSI)
                        .endsWith(HOSTILE_NAME), "the value travels in the environment"),
                () -> assertEquals(appPath, environment.get(WindowsInstallerHandoff.ENV_APP)));
    }

    @Test void commandDoesNotDependOnTheValues(@TempDir Path tmp) {
        // Same command for any input, which is what makes injection impossible.
        List<String> first = WindowsInstallerHandoff.command();
        WindowsInstallerHandoff.environment(tmp.resolve(HOSTILE_NAME), "x", tmp.resolve("l"));

        assertEquals(first, WindowsInstallerHandoff.command());
    }

    @Test void logGoesToLocalAppData() {
        Path home = Path.of("C_home");
        Path local = Path.of("C_local");

        assertEquals(local.resolve("GradeDesk").resolve("update-install.log"),
                WindowsInstallerHandoff.logFile(local.toString(), home));
    }

    @Test void logFallsBackToTheUserProfileWhenLocalAppDataIsMissing() {
        Path home = Path.of("C_home");
        Path expected = home.resolve("AppData").resolve("Local")
                .resolve("GradeDesk").resolve("update-install.log");

        assertAll(
                () -> assertEquals(expected, WindowsInstallerHandoff.logFile(null, home)),
                () -> assertEquals(expected, WindowsInstallerHandoff.logFile("  ", home)));
    }
}
