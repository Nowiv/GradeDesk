package ch.gradedesk.update;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Hands a downloaded MSI over to a helper process that outlives this JVM.
 *
 * <p>An MSI cannot replace files that are still open, and GradeDesk.exe together
 * with its bundled runtime is exactly that while the application runs. Starting
 * msiexec as a child of the JVM therefore fails or returns 3010 ("restart
 * required"), and the restart never happens because the application is still
 * holding its own files.</p>
 *
 * <p>So the work moves out of the JVM: a detached PowerShell process waits for
 * this process to disappear, installs, restarts the application and removes the
 * installer. The application only has to start that helper and then exit.</p>
 *
 * <p><b>On quoting.</b> The script below is a constant. It never has a path
 * pasted into it; every value travels in the helper's environment and is read
 * back with {@code $env:...}. That removes the whole class of problems around
 * spaces, apostrophes in user names and command injection, because there is no
 * string an attacker-controlled value could break out of.</p>
 */
final class WindowsInstallerHandoff {

    /** Environment variable names the script reads its values from. */
    static final String ENV_PID = "GRADEDESK_UPDATE_PID";
    static final String ENV_MSI = "GRADEDESK_UPDATE_MSI";
    static final String ENV_APP = "GRADEDESK_UPDATE_APP";
    static final String ENV_LOG = "GRADEDESK_UPDATE_LOG";

    /** Exit code of msiexec meaning "installed, but Windows wants a reboot". */
    private static final int ERROR_SUCCESS_REBOOT_REQUIRED = 3010;

    /**
     * How long the helper waits for GradeDesk to exit. The application exits
     * right after starting the helper, so this is a safety net, not a delay
     * anyone will normally wait out.
     */
    private static final int WAIT_FOR_EXIT_SECONDS = 180;

    /** Grace period for Windows to release the file handles of the closed app. */
    private static final int HANDLE_RELEASE_MILLIS = 1500;

    /**
     * If the helper is already dead this soon after starting, it never got as far
     * as waiting for us, and the hand-off has to count as failed.
     */
    private static final long STARTUP_PROBE_MILLIS = 600;

    /**
     * The helper script. Constant by design, see the note on quoting above.
     *
     * <p>msiexec is the one place that still needs explicit quotes: it parses its
     * own command line and would otherwise split a path at the first space.
     * Embedding the value in double quotes is safe because a Windows path cannot
     * contain a double quote.</p>
     */
    private static final String SCRIPT = """
            $ErrorActionPreference = 'Stop'

            $target = [int] $env:GRADEDESK_UPDATE_PID
            $msi = $env:GRADEDESK_UPDATE_MSI
            $app = $env:GRADEDESK_UPDATE_APP
            $log = $env:GRADEDESK_UPDATE_LOG

            # Wait until GradeDesk is really gone, so the MSI can replace its files.
            # An already finished process makes Wait-Process throw, which is the
            # good case and needs nothing beyond moving on.
            try { Wait-Process -Id $target -Timeout %d -ErrorAction Stop } catch { }
            Start-Sleep -Milliseconds %d

            $logDirectory = Split-Path -Parent $log
            if ($logDirectory) {
                New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
            }

            $arguments = @('/i', ('"' + $msi + '"'), '/passive', '/norestart',
                           '/l*v', ('"' + $log + '"'))
            $installer = Start-Process -FilePath 'msiexec.exe' -ArgumentList $arguments -Wait -PassThru
            $code = $installer.ExitCode

            if (($code -eq 0 -or $code -eq %d) -and $app -and (Test-Path -LiteralPath $app)) {
                Start-Process -FilePath $app
            }

            Remove-Item -LiteralPath $msi -Force -ErrorAction SilentlyContinue
            """.formatted(WAIT_FOR_EXIT_SECONDS, HANDLE_RELEASE_MILLIS, ERROR_SUCCESS_REBOOT_REQUIRED);

    private WindowsInstallerHandoff() {
    }

    /**
     * Starts the helper and returns once it is up and waiting.
     *
     * @param installer the verified MSI to install
     * @param appPath   value of {@code jpackage.app-path}; an empty value simply
     *                  means the helper installs without restarting anything
     * @param logFile   where msiexec should write its verbose log
     * @throws IOException if the helper could not be started, in which case the
     *                     caller must stay alive and report the failure
     */
    static void start(Path installer, String appPath, Path logFile) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command());
        builder.environment().putAll(environment(installer, appPath, logFile));

        // Nothing is read back from the helper; it reports through its MSI log.
        builder.redirectInput(ProcessBuilder.Redirect.from(new File("NUL")));
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);

        Process helper = builder.start();

        try {
            if (helper.waitFor(STARTUP_PROBE_MILLIS, TimeUnit.MILLISECONDS)) {
                throw new IOException(
                        "Update helper exited immediately with code " + helper.exitValue());
            }
        } catch (InterruptedException e) {
            // The helper is running; being interrupted here says nothing about it.
            Thread.currentThread().interrupt();
        }
    }

    /** The helper command line. Carries no application data, only the script. */
    static List<String> command() {
        return List.of(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-WindowStyle", "Hidden",
                "-EncodedCommand", encodedScript());
    }

    /** The script as PowerShell expects it for {@code -EncodedCommand}: UTF-16LE, no BOM. */
    static String encodedScript() {
        return Base64.getEncoder().encodeToString(SCRIPT.getBytes(StandardCharsets.UTF_16LE));
    }

    /** The values the script reads. This is the only path application data takes. */
    static Map<String, String> environment(Path installer, String appPath, Path logFile) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(ENV_PID, Long.toString(ProcessHandle.current().pid()));
        values.put(ENV_MSI, installer.toAbsolutePath().toString());
        values.put(ENV_APP, appPath == null ? "" : appPath);
        values.put(ENV_LOG, logFile.toAbsolutePath().toString());
        return values;
    }

    /** Where msiexec writes its verbose log, so a failed install stays diagnosable. */
    static Path logFile() {
        return logFile(System.getenv("LOCALAPPDATA"),
                Path.of(System.getProperty("user.home", ".")));
    }

    /** Testable variant of {@link #logFile()} with an explicitly supplied environment. */
    static Path logFile(String localAppData, Path userHome) {
        Path base = localAppData == null || localAppData.isBlank()
                ? userHome.resolve("AppData").resolve("Local")
                : Path.of(localAppData);
        return base.resolve("GradeDesk").resolve("update-install.log");
    }
}
