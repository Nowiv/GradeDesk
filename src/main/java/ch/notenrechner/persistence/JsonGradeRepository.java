package ch.notenrechner.persistence;

import ch.notenrechner.model.Semester;
import ch.notenrechner.persistence.dto.GradeBookDto;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Speichert die Noten als JSON-Datei im Benutzerverzeichnis.
 *
 * <p>Zwei Vorkehrungen schützen vor Datenverlust:</p>
 * <ol>
 *   <li><b>Atomar schreiben.</b> Der neue Stand landet zuerst in einer temporären
 *       Datei. Erst wenn diese vollständig geschrieben ist, wird die bisherige
 *       Datei zum Backup und die temporäre Datei an deren Stelle verschoben.
 *       Ein Absturz mitten im Speichern kann so keine halb geschriebene Datei
 *       hinterlassen.</li>
 *   <li><b>Backup je Speichervorgang.</b> Der jeweils letzte gültige Stand bleibt
 *       als {@code .backup.json} erhalten und wird beim Laden automatisch
 *       verwendet, wenn die Hauptdatei beschädigt ist.</li>
 * </ol>
 *
 * <p>Die Klasse greift ausschliesslich auf das lokale Dateisystem zu. Es gibt
 * keine Netzwerkverbindung, keinen Cloud-Abgleich und keine Telemetrie.</p>
 */
public final class JsonGradeRepository implements GradeRepository {

    /** Name der Hauptdatei. */
    public static final String DATA_FILE_NAME = "noten.json";

    /** Name der Sicherungskopie des zuletzt gültigen Stands. */
    public static final String BACKUP_FILE_NAME = "noten.backup.json";

    private static final String TEMP_FILE_NAME = "noten.json.tmp";
    private static final DateTimeFormatter QUARANTINE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path directory;
    private final ObjectMapper mapper;

    /** Repository im Standard-Datenverzeichnis des angemeldeten Benutzers. */
    public JsonGradeRepository() {
        this(AppPaths.dataDirectory());
    }

    /** Repository in einem beliebigen Verzeichnis (genutzt von den Tests). */
    public JsonGradeRepository(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.mapper = createMapper();
    }

    private static ObjectMapper createMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        // Unbekannte Felder nicht als Fehler behandeln: eine mit einer neueren
        // Version geschriebene Datei bleibt dadurch lesbar, solange das Schema passt.
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper;
    }

    /** Pfad der Hauptdatei. */
    public Path dataFile() {
        return directory.resolve(DATA_FILE_NAME);
    }

    /** Pfad der Sicherungskopie. */
    public Path backupFile() {
        return directory.resolve(BACKUP_FILE_NAME);
    }

    @Override
    public String describeLocation() {
        return dataFile().toAbsolutePath().toString();
    }

    @Override
    public LoadResult load() {
        Path data = dataFile();
        Path backup = backupFile();

        if (Files.exists(data)) {
            try {
                return LoadResult.loaded(read(data));
            } catch (Exception primaryFailure) {
                Path quarantined = quarantine(data);
                String hint = quarantined == null
                        ? ""
                        : " Die beschädigte Datei wurde als «" + quarantined.getFileName() + "» beiseitegelegt.";
                if (Files.exists(backup)) {
                    try {
                        return LoadResult.recovered(read(backup),
                                "Die Notendatei konnte nicht gelesen werden. Es wurde der letzte "
                                        + "gesicherte Stand geladen." + hint);
                    } catch (Exception backupFailure) {
                        return LoadResult.corrupted(
                                "Notendatei und Sicherungskopie konnten nicht gelesen werden. "
                                        + "Notenrechner startet mit einem leeren Notenbuch." + hint);
                    }
                }
                return LoadResult.corrupted("Die Notendatei konnte nicht gelesen werden und es gibt "
                        + "keine Sicherungskopie. Notenrechner startet mit einem leeren Notenbuch." + hint);
            }
        }

        if (Files.exists(backup)) {
            try {
                return LoadResult.recovered(read(backup),
                        "Die Notendatei fehlte. Es wurde der letzte gesicherte Stand geladen.");
            } catch (Exception backupFailure) {
                return LoadResult.corrupted("Die Sicherungskopie konnte nicht gelesen werden. "
                        + "Notenrechner startet mit einem leeren Notenbuch.");
            }
        }

        return LoadResult.noData();
    }

    private List<Semester> read(Path file) throws IOException {
        GradeBookDto dto = mapper.readValue(file.toFile(), GradeBookDto.class);
        if (dto == null) {
            throw new IOException("Die Datei " + file.getFileName() + " ist leer.");
        }
        return GradeBookMapper.toModel(SchemaMigrator.migrate(dto));
    }

    @Override
    public void save(List<Semester> semesters) {
        Objects.requireNonNull(semesters, "semesters");
        GradeBookDto dto = GradeBookMapper.toDto(semesters);
        Path data = dataFile();
        Path backup = backupFile();
        Path temp = directory.resolve(TEMP_FILE_NAME);

        try {
            Files.createDirectories(directory);

            // 1. Vollständig in die temporäre Datei schreiben.
            mapper.writeValue(temp.toFile(), dto);

            // 2. Den bisherigen Stand zum Backup machen.
            if (Files.exists(data)) {
                move(data, backup);
            }

            // 3. Die temporäre Datei an die Stelle der Hauptdatei setzen.
            move(temp, data);
        } catch (IOException e) {
            throw new RepositoryException("Die Noten konnten nicht gespeichert werden: "
                    + e.getMessage(), e);
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Nicht jedes Dateisystem kann atomar verschieben; dann wird ersetzt.
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Legt eine unlesbare Datei zur Seite, statt sie zu überschreiben. So bleibt sie
     * für eine manuelle Rettung erhalten.
     *
     * @return der neue Pfad oder {@code null}, wenn das Umbenennen nicht möglich war
     */
    private Path quarantine(Path file) {
        Path target = directory.resolve("noten.beschaedigt-"
                + LocalDateTime.now().format(QUARANTINE_STAMP) + ".json");
        try {
            return Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return null;
        }
    }
}
