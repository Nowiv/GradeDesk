package ch.gradedesk.update;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Executor;
import java.util.function.BiPredicate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.*;

class UpdateServiceTest {
    @Test void readsChecksumForInstaller() throws Exception {
        String hash = "a".repeat(64);
        assertEquals(hash, UpdateService.findChecksum(hash + "  ./GradeDesk-1.2.3.deb\n", "GradeDesk-1.2.3.deb"));
    }

    @Test void rejectsMissingOrMalformedChecksum() {
        assertThrows(IOException.class, () -> UpdateService.findChecksum("not-a-checksum file.deb", "file.deb"));
        assertThrows(IOException.class, () -> UpdateService.findChecksum("a".repeat(64) + "  other.deb", "file.deb"));
    }

    @Test void selectsPlatformSpecificInstallerExtension() {
        String previous = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 11"); assertEquals(".msi", UpdateService.installerExtension());
            System.setProperty("os.name", "Mac OS X"); assertEquals(".dmg", UpdateService.installerExtension());
            System.setProperty("os.name", "Linux"); assertEquals(".deb", UpdateService.installerExtension());
        } finally {
            if (previous == null) System.clearProperty("os.name"); else System.setProperty("os.name", previous);
        }
    }

    @Test void downloadsInstallerAndVerifiesSha256() throws Exception {
        byte[] installer = "test-installer-content".getBytes(StandardCharsets.UTF_8);
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(installer));
        String sums = checksum + "  ./GradeDesk-1.2.3.deb\n";
        UpdateService service = new UpdateService(new StubHttpClient(Map.of(
                "https://example.test/SHA256SUMS.txt", sums.getBytes(StandardCharsets.UTF_8),
                "https://example.test/GradeDesk-1.2.3.deb", installer)));
        UpdateService.CheckResult result = new UpdateService.CheckResult(UpdateService.Status.UPDATE_AVAILABLE,
                SemVer.parse("1.0.0"), SemVer.parse("1.2.3"), "", "https://example.test/GradeDesk-1.2.3.deb",
                "https://example.test/SHA256SUMS.txt", "GradeDesk-1.2.3.deb");

        Path downloaded = service.downloadAndVerify(result, null);
        try { assertArrayEquals(installer, Files.readAllBytes(downloaded)); }
        finally { Files.deleteIfExists(downloaded); }
    }

    @Test void deletesInstallerWhenChecksumDoesNotMatch() throws Exception {
        byte[] installer = "tampered-installer".getBytes(StandardCharsets.UTF_8);
        String sums = "0".repeat(64) + "  ./GradeDesk-1.2.3.deb\n";
        UpdateService service = new UpdateService(new StubHttpClient(Map.of(
                "https://example.test/SHA256SUMS.txt", sums.getBytes(StandardCharsets.UTF_8),
                "https://example.test/GradeDesk-1.2.3.deb", installer)));
        UpdateService.CheckResult result = new UpdateService.CheckResult(UpdateService.Status.UPDATE_AVAILABLE,
                SemVer.parse("1.0.0"), SemVer.parse("1.2.3"), "", "https://example.test/GradeDesk-1.2.3.deb",
                "https://example.test/SHA256SUMS.txt", "GradeDesk-1.2.3.deb");

        assertThrows(IOException.class, () -> service.downloadAndVerify(result, null));
    }

    private static final class StubHttpClient extends HttpClient {
        private final Map<String, byte[]> responses;
        private StubHttpClient(Map<String, byte[]> responses) { this.responses = responses; }
        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }

        @Override public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
                throws IOException {
            byte[] bytes = responses.get(request.uri().toString());
            if (bytes == null) throw new IOException("Unexpected test URL: " + request.uri());
            HttpHeaders headers = HttpHeaders.of(Map.of("Content-Length", List.of(String.valueOf(bytes.length))), (a, b) -> true);
            HttpResponse.ResponseInfo info = new HttpResponse.ResponseInfo() {
                @Override public int statusCode() { return 200; }
                @Override public HttpHeaders headers() { return headers; }
                @Override public Version version() { return Version.HTTP_1_1; }
            };
            HttpResponse.BodySubscriber<T> subscriber = handler.apply(info);
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override public void request(long n) { }
                @Override public void cancel() { }
            });
            subscriber.onNext(List.of(ByteBuffer.wrap(bytes)));
            subscriber.onComplete();
            T body = subscriber.getBody().toCompletableFuture().join();
            return new StubResponse<>(request, headers, body);
        }

        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                HttpResponse.BodyHandler<T> handler) { throw new UnsupportedOperationException(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                HttpResponse.BodyHandler<T> handler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
        }
    }

    private record StubResponse<T>(HttpRequest request, HttpHeaders headers, T body) implements HttpResponse<T> {
        @Override public int statusCode() { return 200; }
        @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request.uri(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
