package ch.gradedesk.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.function.LongConsumer;

/** GitHub-release based update discovery and verified installer download. */
public final class UpdateService {
    public static final String OWNER = "Nowiv";
    public static final String REPOSITORY = "GradeHub";
    private static final URI LATEST_API = URI.create("https://api.github.com/repos/" + OWNER + "/" + REPOSITORY + "/releases/latest");
    private static final String USER_AGENT = "GradeDesk-Updater/1.0";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public UpdateService() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    UpdateService(HttpClient client) { this.client = client; }

    public Optional<SemVer> installedVersion() {
        String value = System.getProperty("jpackage.app-version");
        if (value == null || value.isBlank()) return Optional.empty();
        try { return Optional.of(SemVer.parse(value)); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }

    public CheckResult check() throws IOException, InterruptedException {
        Optional<SemVer> current = installedVersion();
        if (current.isEmpty()) return CheckResult.ideLaunch();
        HttpRequest request = HttpRequest.newBuilder(LATEST_API).timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", USER_AGENT).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 403 || response.statusCode() == 429) return CheckResult.rateLimited();
        if (response.statusCode() == 404) return CheckResult.noRelease();
        if (response.statusCode() != 200) throw new IOException("GitHub API returned HTTP " + response.statusCode());
        JsonNode root = mapper.readTree(response.body());
        if (root.path("draft").asBoolean(true) || root.path("prerelease").asBoolean(true)) return CheckResult.noRelease();
        String tag = root.path("tag_name").asText("");
        if (tag.startsWith("v")) tag = tag.substring(1);
        SemVer latest;
        try { latest = SemVer.parse(tag); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid release version: " + tag, e); }
        if (latest.compareTo(current.get()) <= 0) return CheckResult.upToDate(current.get(), latest);
        String body = root.path("body").asText("");
        String checksumUrl = null;
        String installerUrl = null;
        String extension = installerExtension();
        for (JsonNode asset : root.path("assets")) {
            String name = asset.path("name").asText("");
            String url = asset.path("browser_download_url").asText("");
            if (name.equals("SHA256SUMS.txt")) checksumUrl = url;
            if (!extension.isEmpty() && name.toLowerCase(Locale.ROOT).endsWith(extension)) installerUrl = url;
        }
        if (installerUrl == null || checksumUrl == null) throw new IOException("Release is missing installer or SHA256SUMS.txt");
        return CheckResult.available(current.get(), latest, body, installerUrl, checksumUrl,
                installerUrl.substring(installerUrl.lastIndexOf('/') + 1));
    }

    public Path downloadAndVerify(CheckResult update, LongConsumer progress) throws IOException, InterruptedException {
        if (!update.available()) throw new IllegalArgumentException("No update available");
        Path target = Files.createTempFile("gradedesk-update-", "-" + update.assetName());
        try {
            HttpRequest checksumRequest = HttpRequest.newBuilder(URI.create(update.checksumUrl())).timeout(TIMEOUT)
                    .header("User-Agent", USER_AGENT).GET().build();
            HttpResponse<String> checksumResponse = client.send(checksumRequest, HttpResponse.BodyHandlers.ofString());
            if (checksumResponse.statusCode() != 200) throw new IOException("Could not download checksums (HTTP " + checksumResponse.statusCode() + ")");
            String expected = findChecksum(checksumResponse.body(), update.assetName());
            HttpRequest fileRequest = HttpRequest.newBuilder(URI.create(update.installerUrl())).timeout(Duration.ofMinutes(10))
                    .header("User-Agent", USER_AGENT).GET().build();
            HttpResponse<InputStream> fileResponse = client.send(fileRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (fileResponse.statusCode() != 200) throw new IOException("Installer download failed (HTTP " + fileResponse.statusCode() + ")");
            long total = fileResponse.headers().firstValueAsLong("Content-Length").orElse(-1);
            MessageDigest digest = sha256();
            long copied = 0;
            try (InputStream in = fileResponse.body(); var out = Files.newOutputStream(target)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                    copied += read;
                    if (progress != null) progress.accept(total > 0 ? copied * 100 / total : -1);
                }
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    actual.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                throw new IOException("SHA-256 checksum mismatch; installer was discarded");
            }
            return target;
        } catch (IOException | InterruptedException | RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    static String findChecksum(String contents, String filename) throws IOException {
        for (String line : contents.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length == 2 && parts[0].matches("(?i)[0-9a-f]{64}")) {
                String listed = parts[1].replaceFirst("^[* ]+", "");
                listed = Path.of(listed).getFileName().toString();
                if (listed.equals(filename)) return parts[0].toLowerCase(Locale.ROOT);
            }
        }
        throw new IOException("No SHA-256 entry for " + filename);
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    static String installerExtension() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return ".msi";
        if (os.contains("mac") || os.contains("darwin")) return ".dmg";
        if (os.contains("linux")) return ".deb";
        return "";
    }

    public record CheckResult(Status status, SemVer current, SemVer latest, String releaseNotes,
                              String installerUrl, String checksumUrl, String assetName) {
        public boolean available() { return status == Status.UPDATE_AVAILABLE; }
        static CheckResult ideLaunch() { return new CheckResult(Status.IDE_LAUNCH, null, null, "", null, null, null); }
        static CheckResult rateLimited() { return new CheckResult(Status.RATE_LIMITED, null, null, "", null, null, null); }
        static CheckResult noRelease() { return new CheckResult(Status.NO_RELEASE, null, null, "", null, null, null); }
        static CheckResult upToDate(SemVer c, SemVer l) { return new CheckResult(Status.UP_TO_DATE, c, l, "", null, null, null); }
        static CheckResult available(SemVer c, SemVer l, String notes, String url, String checksums, String name) {
            return new CheckResult(Status.UPDATE_AVAILABLE, c, l, notes, url, checksums, name);
        }
    }
    public enum Status { IDE_LAUNCH, RATE_LIMITED, NO_RELEASE, UP_TO_DATE, UPDATE_AVAILABLE }
}
