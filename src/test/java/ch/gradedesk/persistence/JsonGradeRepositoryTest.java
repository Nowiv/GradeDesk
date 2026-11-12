package ch.gradedesk.persistence;

import ch.gradedesk.model.CourseModule;
import ch.gradedesk.persistence.dto.GradeBookDto;
import ch.gradedesk.model.Grade;
import ch.gradedesk.model.PlannedExam;
import ch.gradedesk.model.Semester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonGradeRepositoryTest {

    @TempDir
    Path dataDir;

    private JsonGradeRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JsonGradeRepository(dataDir);
    }

    private static List<Semester> sampleData() {
        Semester semester = new Semester("sem-1", "HS 2025");
        CourseModule math = new CourseModule("mod-1", "Mathematik");
        math.getGrades().add(new Grade("grade-1", "Prüfung 1",
                new BigDecimal("4.5"), new BigDecimal("2"), LocalDate.of(2025, 9, 12)));
        math.getGrades().add(new Grade("grade-2", "Prüfung 2",
                new BigDecimal("3.5"), BigDecimal.ONE, LocalDate.of(2025, 11, 3)));
        math.getPlannedExams().add(new PlannedExam("exam-1", "Prüfung 3",
                new BigDecimal("2"), LocalDate.of(2026, 1, 20)));
        semester.setCompleted(true);
        CourseModule empty = new CourseModule("mod-2", "Sport");
        empty.getPlannedExams().add(new PlannedExam("exam-2", "",
                BigDecimal.ONE, LocalDate.of(2026, 2, 3)));
        semester.getModules().addAll(math, empty);
        return List.of(semester);
    }

    @Test
    @DisplayName("Ohne gespeicherte Datei startet die Anwendung leer")
    void loadWithoutFileReportsNoData() {
        LoadResult result = repository.load();

        assertEquals(LoadResult.Status.NO_DATA, result.status());
        assertTrue(result.semesters().isEmpty());
        assertFalse(result.needsAttention());
    }

    @Test
    @DisplayName("Gespeicherte Daten werden unverändert wieder geladen")
    void savesAndLoadsRoundTrip() {
        repository.save(sampleData());

        LoadResult result = repository.load();

        assertEquals(LoadResult.Status.LOADED, result.status());
        assertEquals(1, result.semesters().size());

        Semester semester = result.semesters().get(0);
        assertEquals("sem-1", semester.getId());
        assertEquals("HS 2025", semester.getName());
        assertEquals(2, semester.getModules().size());

        CourseModule math = semester.getModules().get(0);
        assertEquals("Mathematik", math.getName());
        assertEquals(2, math.getGrades().size());

        Grade first = math.getGrades().get(0);
        assertEquals("grade-1", first.getId());
        assertEquals("Prüfung 1", first.getLabel());
        assertEquals(0, first.getValue().compareTo(new BigDecimal("4.5")));
        assertEquals(0, first.getWeight().compareTo(new BigDecimal("2")));
        assertEquals(LocalDate.of(2025, 9, 12), first.getDate());

        // (4.5*2 + 3.5*1) / 3 = 4.1666... -> gerundet 4.0
        assertEquals(new BigDecimal("4.0"), math.getRoundedGrade());

        // Ein Modul ohne Noten bleibt ohne Note.
        assertEquals("Sport", semester.getModules().get(1).getName());
        assertFalse(semester.getModules().get(1).hasGrade());
    }

    @Test
    @DisplayName("Die Datei enthält eine Schema-Version und liegt im Datenverzeichnis")
    void writesSchemaVersion() throws IOException {
        repository.save(sampleData());

        Path file = dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME);
        assertTrue(Files.exists(file));
        String json = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"schemaVersion\" : "
                + GradeBookDto.CURRENT_SCHEMA_VERSION), json);
        assertTrue(json.contains("\"2025-09-12\""), "Datum als ISO-Text erwartet: " + json);
    }

    @Test
    @DisplayName("Jedes Speichern sichert den vorherigen Stand als Backup")
    void keepsBackupOfPreviousVersion() throws IOException {
        repository.save(sampleData());

        Semester second = new Semester("sem-2", "FS 2026");
        repository.save(List.of(second));

        Path backup = dataDir.resolve(JsonGradeRepository.BACKUP_FILE_NAME);
        assertTrue(Files.exists(backup), "Backup erwartet");
        assertTrue(Files.readString(backup, StandardCharsets.UTF_8).contains("HS 2025"));
        assertTrue(Files.readString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME),
                StandardCharsets.UTF_8).contains("FS 2026"));
    }

    @Test
    @DisplayName("Nach dem Speichern bleibt keine temporäre Datei liegen")
    void leavesNoTemporaryFile() throws IOException {
        repository.save(sampleData());

        try (Stream<Path> files = Files.list(dataDir)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    @DisplayName("Beschädigte Datei: der letzte gesicherte Stand wird geladen")
    void recoversFromBackupWhenFileIsBroken() throws IOException {
        repository.save(sampleData());
        // Zweites Speichern, damit der erste Stand im Backup liegt.
        repository.save(List.of(new Semester("sem-2", "FS 2026")));

        Files.writeString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME),
                "{ das ist kein gültiges JSON", StandardCharsets.UTF_8);

        LoadResult result = repository.load();

        assertEquals(LoadResult.Status.RECOVERED_FROM_BROKEN_FILE, result.status());
        assertTrue(result.needsAttention());
        assertNotNull(result.detail(), "Der Name der beiseitegelegten Datei wird gemeldet");
        assertEquals(1, result.semesters().size());
        assertEquals("HS 2025", result.semesters().get(0).getName());
    }

    @Test
    @DisplayName("Beschädigte Datei wird beiseitegelegt, nicht überschrieben")
    void movesBrokenFileAside() throws IOException {
        repository.save(sampleData());
        Files.writeString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME),
                "kaputt", StandardCharsets.UTF_8);

        repository.load();

        try (Stream<Path> files = Files.list(dataDir)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("grades.corrupt-")),
                    "Die unlesbare Datei soll erhalten bleiben");
        }
    }

    @Test
    @DisplayName("Ohne lesbares Backup startet die Anwendung leer und meldet das")
    void reportsCorruptionWhenNothingIsReadable() throws IOException {
        Files.writeString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME),
                "kaputt", StandardCharsets.UTF_8);

        LoadResult result = repository.load();

        assertEquals(LoadResult.Status.CORRUPTED, result.status());
        assertTrue(result.semesters().isEmpty());
        assertTrue(result.needsAttention());
    }

    @Test
    @DisplayName("Fehlt die Hauptdatei, springt das Backup ein")
    void fallsBackToBackupWhenMainFileIsMissing() throws IOException {
        repository.save(sampleData());
        repository.save(List.of(new Semester("sem-2", "FS 2026")));
        Files.delete(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME));

        LoadResult result = repository.load();

        assertEquals(LoadResult.Status.RECOVERED_FROM_MISSING_FILE, result.status());
        assertEquals("HS 2025", result.semesters().get(0).getName());
    }

    @Test
    @DisplayName("Eine Datei aus einer neueren Programmversion wird nicht stillschweigend geöffnet")
    void refusesNewerSchemaVersion() throws IOException {
        Files.writeString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME),
                "{ \"schemaVersion\": 99, \"semesters\": [] }", StandardCharsets.UTF_8);

        LoadResult result = repository.load();

        assertEquals(LoadResult.Status.CORRUPTED, result.status());
    }

    @Test
    @DisplayName("Unvollständige Noten werden beim Laden übersprungen")
    void skipsGradesWithoutValue() throws IOException {
        Files.writeString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME), """
                {
                  "schemaVersion": 1,
                  "semesters": [
                    { "id": "s1", "name": "HS 2025", "modules": [
                      { "id": "m1", "name": "Mathematik", "grades": [
                        { "id": "g1", "label": "Mit Wert", "value": 5.0, "weight": 1, "date": "2025-09-01" },
                        { "id": "g2", "label": "Ohne Wert", "weight": 1, "date": "2025-09-02" }
                      ]}
                    ]}
                  ]
                }
                """, StandardCharsets.UTF_8);

        LoadResult result = repository.load();

        assertEquals(LoadResult.Status.LOADED, result.status());
        CourseModule module = result.semesters().get(0).getModules().get(0);
        assertEquals(1, module.getGrades().size());
        assertEquals("Mit Wert", module.getGrades().get(0).getLabel());
    }

    @Test
    @DisplayName("Der Stempel «abgeschlossen» wird mitgespeichert und wieder geladen")
    void savesAndLoadsTheCompletedStamp() {
        repository.save(sampleData());

        LoadResult result = repository.load();

        assertTrue(result.semesters().get(0).isCompleted());
    }

    @Test
    @DisplayName("Eine Datei der Version 2 kommt mit offenen Semestern an")
    void semestersOfOlderFilesAreOpen() throws IOException {
        Files.writeString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME), """
                {
                  "schemaVersion": 2,
                  "semesters": [
                    { "id": "s1", "name": "HS 2025", "modules": [
                      { "id": "m1", "name": "Mathematik", "grades": [
                        { "id": "g1", "label": "Prüfung 1", "value": 5.0, "weight": 1, "date": "2025-09-01" }
                      ]}
                    ]}
                  ]
                }
                """, StandardCharsets.UTF_8);

        LoadResult result = repository.load();

        assertEquals(LoadResult.Status.LOADED, result.status());
        assertFalse(result.semesters().get(0).isCompleted());
        assertEquals(1, result.semesters().get(0).getModules().get(0).getGrades().size());
    }

    @Test
    @DisplayName("Der Speicherort liegt im angegebenen Datenverzeichnis")
    void describesItsLocation() {
        assertTrue(repository.describeLocation()
                .endsWith(JsonGradeRepository.DATA_FILE_NAME));
        assertTrue(repository.describeLocation().contains(dataDir.getFileName().toString()));
    }

    @Test
    @DisplayName("Geplante Prüfungen werden mitgespeichert und wieder geladen")
    void savesAndLoadsPlannedExams() {
        repository.save(sampleData());

        LoadResult result = repository.load();
        CourseModule math = result.semesters().get(0).getModules().get(0);

        assertEquals(1, math.getPlannedExams().size());
        PlannedExam exam = math.getPlannedExams().get(0);
        assertEquals("exam-1", exam.getId());
        assertEquals("Prüfung 3", exam.getLabel());
        assertEquals(0, exam.getWeight().compareTo(new BigDecimal("2")));
        assertEquals(LocalDate.of(2026, 1, 20), exam.getDate());

        // Die Modulnote beruht weiterhin nur auf den Noten.
        assertEquals(new BigDecimal("4.0"), math.getRoundedGrade());

        // Eine geplante Prüfung ohne Bezeichnung bleibt ohne Bezeichnung.
        CourseModule sport = result.semesters().get(0).getModules().get(1);
        assertEquals(1, sport.getPlannedExams().size());
        assertEquals("", sport.getPlannedExams().get(0).getLabel());
        assertFalse(sport.hasGrade());
    }

    @Test
    @DisplayName("Eine Datei der Version 1 wird gelesen und hat keine geplanten Prüfungen")
    void readsFilesOfTheOlderSchemaVersion() throws IOException {
        Files.writeString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME), """
                {
                  "schemaVersion": 1,
                  "semesters": [
                    { "id": "s1", "name": "HS 2025", "modules": [
                      { "id": "m1", "name": "Mathematik", "grades": [
                        { "id": "g1", "label": "Prüfung 1", "value": 5.0, "weight": 1, "date": "2025-09-01" }
                      ]}
                    ]}
                  ]
                }
                """, StandardCharsets.UTF_8);

        LoadResult result = repository.load();

        assertEquals(LoadResult.Status.LOADED, result.status());
        CourseModule module = result.semesters().get(0).getModules().get(0);
        assertEquals(1, module.getGrades().size());
        assertTrue(module.getPlannedExams().isEmpty());
        assertEquals(new BigDecimal("5.0"), module.getRoundedGrade());
    }

    @Test
    @DisplayName("Nach dem Laden einer alten Datei wird in der neuen Version gespeichert")
    void writesTheNewVersionAfterMigration() throws IOException {
        Files.writeString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME), """
                {
                  "schemaVersion": 1,
                  "semesters": [
                    { "id": "s1", "name": "HS 2025", "modules": [] }
                  ]
                }
                """, StandardCharsets.UTF_8);

        LoadResult result = repository.load();
        repository.save(result.semesters());

        String json = Files.readString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("\"schemaVersion\" : " + GradeBookDto.CURRENT_SCHEMA_VERSION), json);
    }

    @Test
    @DisplayName("Eine geplante Prüfung ohne Datum wird beim Laden übersprungen")
    void skipsPlannedExamsWithoutDate() throws IOException {
        Files.writeString(dataDir.resolve(JsonGradeRepository.DATA_FILE_NAME), """
                {
                  "schemaVersion": 2,
                  "semesters": [
                    { "id": "s1", "name": "HS 2025", "modules": [
                      { "id": "m1", "name": "Mathematik", "grades": [], "plannedExams": [
                        { "id": "p1", "label": "Mit Datum", "weight": 1, "date": "2026-01-20" },
                        { "id": "p2", "label": "Ohne Datum", "weight": 1 }
                      ]}
                    ]}
                  ]
                }
                """, StandardCharsets.UTF_8);

        LoadResult result = repository.load();

        CourseModule module = result.semesters().get(0).getModules().get(0);
        assertEquals(1, module.getPlannedExams().size());
        assertEquals("Mit Datum", module.getPlannedExams().get(0).getLabel());
    }
}
