package ch.gradedesk.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.function.LongConsumer;

/**
 * Discovers GitHub releases and downloads installers to a temporary file.
 * A downloaded installer is returned only after its SHA-256 digest is verified.
 */
public final class UpdateService {
    public static final String OWNER = "Nowiv";
    public static final String REPOSITORY = "GradeHub";

    private static final URI LATEST_API = URI.create(
            "https://api.github.com/repos/" + OWNER + "/" + REPOSITORY + "/releases/latest");
    private static final String USER_AGENT = "GradeDesk-Updater/1.1";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration API_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public UpdateService() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    UpdateService(HttpClient client) {
        this.client = client;
    }

    public Optional<SemVer> installedVersion() {
        // Keep your existing version-detection strategy for now.
        // Verify this property in a packaged installation; it may be absent at runtime.
        String value = System.getProperty("jpackage.app-version");
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(SemVer.parse(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public CheckResult check() throws IOException, InterruptedException {
        Optional<SemVer> current = installedVersion();
        if (current.isEmpty()) {
            return CheckResult.ideLaunch();
        }

        HttpRequest request = HttpRequest.newBuilder(LATEST_API)
                .timeout(API_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() == 403 || response.statusCode() == 429) {
            return CheckResult.rateLimited();
        }
        if (response.statusCode() == 404) {
            return CheckResult.noRelease();
        }
        if (response.statusCode() != 200) {
            throw new IOException("GitHub API returned HTTP " + response.statusCode());
        }

        JsonNode root = mapper.readTree(response.body());
        if (root.path("draft").asBoolean(true) || root.path("prerelease").asBoolean(true)) {
            return CheckResult.noRelease();
        }

        String tag = root.path("tag_name").asText("");
        if (tag.startsWith("v")) {
            tag = tag.substring(1);
        }

        final SemVer latest;
        try {
            latest = SemVer.parse(tag);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid release version: " + tag, e);
        }

        if (latest.compareTo(current.get()) <= 0) {
            return CheckResult.upToDate(current.get(), latest);
        }

        String releaseNotes = root.path("body").asText("");
        String checksumUrl = null;
        String installerUrl = null;
        String extension = installerExtension();

        for (JsonNode asset : root.path("assets")) {
            String name = asset.path("name").asText("");
            String url = asset.path("browser_download_url").asText("");

            if ("SHA256SUMS.txt".equals(name)) {
                checksumUrl = url;
            }
            if (!extension.isEmpty() && name.toLowerCase(Locale.ROOT).endsWith(extension)) {
                installerUrl = url;
            }
        }

        if (installerUrl == null || checksumUrl == null) {
            throw new IOException("Release is missing this platform's installer or SHA256SUMS.txt");
        }

        requireHttps(installerUrl);
        requireHttps(checksumUrl);

        String assetName = installerUrl.substring(installerUrl.lastIndexOf('/') + 1);
        if (assetName.isBlank()) {
            throw new IOException("GitHub release asset has no filename");
        }

        return CheckResult.available(
                current.get(), latest, releaseNotes, installerUrl, checksumUrl, assetName);
    }

    /**
     * Downloads an installer, verifies its SHA-256 checksum and returns its path.
     * The progress callback receives 0..100, or -1 if the server did not send a size.
     */
    public Path downloadAndVerify(CheckResult update, LongConsumer progress)
            throws IOException, InterruptedException {
        if (update == null || !update.available()) {
            throw new IllegalArgumentException("No update available");
        }

        requireHttps(update.installerUrl());
        requireHttps(update.checksumUrl());

        String expected = downloadExpectedChecksum(update);
        Path partFile = Files.createTempFile("gradedesk-update-", ".part");
        Path verifiedFile = null;

        try {
            downloadInstallerWithRetries(update, expected, partFile, progress);

            String safeName = Path.of(update.assetName()).getFileName().toString();
            verifiedFile = Files.createTempFile("gradedesk-verified-", "-" + safeName);
            Files.deleteIfExists(verifiedFile);

            try {
                Files.move(partFile, verifiedFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(partFile, verifiedFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return verifiedFile;
        } catch (IOException | InterruptedException | RuntimeException e) {
            Files.deleteIfExists(partFile);
            if (verifiedFile != null) {
                Files.deleteIfExists(verifiedFile);
            }
            throw e;
        }
    }

    private String downloadExpectedChecksum(CheckResult update)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(update.checksumUrl()))
                .timeout(API_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        IOException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() == 200) {
                    requireHttps(response.uri().toString());
                    return findChecksum(response.body(), update.assetName());
                }

                IOException error = new IOException(
                        "Checksum download failed (HTTP " + response.statusCode() + ")");
                if (!isRetryable(response.statusCode()) || attempt == MAX_ATTEMPTS) {
                    throw error;
                }
                lastError = error;
            } catch (IOException e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                lastError = e;
            }

            pauseBeforeRetry(attempt);
        }

        throw lastError == null ? new IOException("Checksum download failed") : lastError;
    }

    private void downloadInstallerWithRetries(
            CheckResult update, String expected, Path partFile, LongConsumer progress)
            throws IOException, InterruptedException {
        IOException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Files.deleteIfExists(partFile);
                Files.createFile(partFile);

                HttpRequest request = HttpRequest.newBuilder(URI.create(update.installerUrl()))
                        .timeout(DOWNLOAD_TIMEOUT)
                        .header("User-Agent", USER_AGENT)
                        .GET()
                        .build();

                HttpResponse<InputStream> response = client.send(
                        request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    try (InputStream ignored = response.body()) {
                        // Close response body before retrying/failing.
                    }
                    IOException error = new IOException(
                            "Installer download failed (HTTP " + response.statusCode() + ")");
                    if (!isRetryable(response.statusCode()) || attempt == MAX_ATTEMPTS) {
                        throw error;
                    }
                    lastError = error;
                } else {
                    requireHttps(response.uri().toString());

                    long expectedLength = response.headers()
                            .firstValueAsLong("Content-Length").orElse(-1L);
                    MessageDigest digest = sha256();
                    long received = 0;

                    try (InputStream in = response.body();
                         var out = Files.newOutputStream(partFile)) {
                        byte[] buffer = new byte[64 * 1024];
                        int count;
                        while ((count = in.read(buffer)) != -1) {
                            out.write(buffer, 0, count);
                            digest.update(buffer, 0, count);
                            received += count;

                            if (progress != null) {
                                progress.accept(expectedLength > 0
                                        ? Math.min(99, received * 100 / expectedLength)
                                        : -1);
                            }
                        }
                    }

                    if (expectedLength >= 0 && received != expectedLength) {
                        throw new IOException("Download incomplete: expected "
                                + expectedLength + " bytes but received " + received);
                    }

                    String actual = HexFormat.of().formatHex(digest.digest());
                    if (!MessageDigest.isEqual(
                            expected.getBytes(StandardCharsets.US_ASCII),
                            actual.getBytes(StandardCharsets.US_ASCII))) {
                        throw new IOException("SHA-256 checksum mismatch; installer was discarded");
                    }

                    if (progress != null) {
                        progress.accept(100);
                    }
                    return;
                }
            } catch (IOException e) {
                Files.deleteIfExists(partFile);
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                lastError = e;
            }

            pauseBeforeRetry(attempt);
        }

        throw lastError == null ? new IOException("Installer download failed") : lastError;
    }

    private static boolean isRetryable(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private static void pauseBeforeRetry(int attempt) throws InterruptedException {
        Thread.sleep(500L * attempt);
    }

    static String findChecksum(String contents, String filename) throws IOException {
        for (String line : contents.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length == 2 && parts[0].matches("(?i)[0-9a-f]{64}")) {
                String listed = parts[1].replaceFirst("^[* ]+", "");
                listed = Path.of(listed).getFileName().toString();
                if (listed.equals(filename)) {
                    return parts[0].toLowerCase(Locale.ROOT);
                }
            }
        }
        throw new IOException("No SHA-256 entry for " + filename);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static void requireHttps(String value) throws IOException {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IOException("Refusing non-HTTPS update URL");
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid update URL", e);
        }
    }

    static String installerExtension() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return ".msi";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return ".dmg";
        }
        if (os.contains("linux")) {
            return ".deb";
        }
        return "";
    }

    public record CheckResult(
            Status status,
            SemVer current,
            SemVer latest,
            String releaseNotes,
            String installerUrl,
            String checksumUrl,
            String assetName) {

        public boolean available() {
            return status == Status.UPDATE_AVAILABLE;
        }

        static CheckResult ideLaunch() {
            return new CheckResult(Status.IDE_LAUNCH, null, null, "", null, null, null);
        }

        static CheckResult rateLimited() {
            return new CheckResult(Status.RATE_LIMITED, null, null, "", null, null, null);
        }

        static CheckResult noRelease() {
            return new CheckResult(Status.NO_RELEASE, null, null, "", null, null, null);
        }

        static CheckResult upToDate(SemVer current, SemVer latest) {
            return new CheckResult(Status.UP_TO_DATE, current, latest, "", null, null, null);
        }

        static CheckResult available(
                SemVer current, SemVer latest, String notes,
                String url, String checksums, String name) {
            return new CheckResult(
                    Status.UPDATE_AVAILABLE, current, latest, notes, url, checksums, name);
        }
    }

    public enum Status {
        IDE_LAUNCH,
        RATE_LIMITED,
        NO_RELEASE,
        UP_TO_DATE,
        UPDATE_AVAILABLE
    }
}
