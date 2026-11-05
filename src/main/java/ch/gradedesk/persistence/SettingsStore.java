package ch.gradedesk.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Speichert die Programmeinstellungen – zurzeit nur die gewählte Sprache.
 *
 * <p>Die Einstellungen liegen als eigene Datei neben den Noten, im selben
 * Benutzerverzeichnis. Bewusst nicht in der Registry oder über
 * {@code java.util.prefs}: so liegt alles, was die Anwendung über eine Person
 * weiss, an einer einzigen, auffindbaren Stelle.</p>
 *
 * <p>Fehler beim Lesen oder Schreiben sind hier nicht kritisch – im
 * schlimmsten Fall startet die Anwendung in der Systemsprache. Deshalb werden
 * sie geschluckt statt weitergereicht.</p>
 */
public final class SettingsStore {

    /** Name der Einstellungsdatei. */
    public static final String FILE_NAME = "settings.json";

    private final Path directory;
    private final ObjectMapper mapper;

    /** Einstellungen im Standard-Datenverzeichnis des angemeldeten Benutzers. */
    public SettingsStore() {
        this(AppPaths.dataDirectory());
    }

    /** Einstellungen in einem beliebigen Verzeichnis (genutzt von den Tests). */
    public SettingsStore(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /** Pfad der Einstellungsdatei. */
    public Path file() {
        return directory.resolve(FILE_NAME);
    }

    /** Liest die Einstellungen, oder {@link Optional#empty()}, wenn es noch keine gibt. */
    public Optional<AppSettings> load() {
        Path file = file();
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(mapper.readValue(file.toFile(), AppSettings.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Schreibt die Einstellungen. Schlägt das fehl, bleibt es bei den Standardwerten. */
    public void save(AppSettings settings) {
        try {
            Files.createDirectories(directory);
            mapper.writeValue(file().toFile(), settings);
        } catch (IOException e) {
            // Eine nicht gespeicherte Spracheinstellung ist ein kleiner Schönheitsfehler,
            // kein Grund, die laufende Arbeit zu unterbrechen.
        }
    }

    /**
     * Die gespeicherten Einstellungen.
     *
     * @param schemaVersion Format dieser Datei, für spätere Erweiterungen
     * @param language      Sprachkennung, z.B. {@code de} oder {@code en}
     */
    public record AppSettings(int schemaVersion, String language) {

        /** Aktuelle Version des Einstellungsformats. */
        public static final int CURRENT_SCHEMA_VERSION = 1;

        public static AppSettings of(String language) {
            return new AppSettings(CURRENT_SCHEMA_VERSION, language);
        }
    }
}
