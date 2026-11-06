package ch.gradedesk.persistence;

import ch.gradedesk.persistence.dto.GradeBookDto;
import ch.gradedesk.persistence.dto.GradeDto;
import ch.gradedesk.persistence.dto.ModuleDto;
import ch.gradedesk.persistence.dto.PlannedExamDto;
import ch.gradedesk.persistence.dto.SemesterDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Die Übersetzung alter Dateiformate auf den aktuellen Stand. */
class SchemaMigratorTest {

    private static final LocalDate DATE = LocalDate.of(2025, 9, 12);

    private static GradeBookDto fileOfVersion(int version, List<PlannedExamDto> plannedExams) {
        ModuleDto module = new ModuleDto("m1", "Mathematik",
                List.of(new GradeDto("g1", "Prüfung 1", new BigDecimal("4.5"),
                        BigDecimal.ONE, DATE)),
                plannedExams);
        return new GradeBookDto(version,
                List.of(new SemesterDto("s1", "HS 2025", List.of(module))));
    }

    private static ModuleDto firstModule(GradeBookDto dto) {
        return dto.semesters().get(0).modules().get(0);
    }

    @Test
    @DisplayName("Version 1: alle Semester kommen ohne geplante Prüfungen an")
    void version1GetsEmptyPlannedExams() {
        GradeBookDto migrated = SchemaMigrator.migrate(fileOfVersion(1, null));

        assertEquals(GradeBookDto.CURRENT_SCHEMA_VERSION, migrated.schemaVersion());
        assertTrue(firstModule(migrated).plannedExams().isEmpty());
    }

    @Test
    @DisplayName("Version 1: Semester, Module und Noten bleiben unverändert")
    void version1KeepsEverythingElse() {
        GradeBookDto migrated = SchemaMigrator.migrate(fileOfVersion(1, null));

        assertEquals(1, migrated.semesters().size());
        assertEquals("HS 2025", migrated.semesters().get(0).name());
        assertEquals("Mathematik", firstModule(migrated).name());
        assertEquals(1, firstModule(migrated).grades().size());
        assertEquals("Prüfung 1", firstModule(migrated).grades().get(0).label());
        assertEquals(DATE, firstModule(migrated).grades().get(0).date());
    }

    @Test
    @DisplayName("Eine Datei ohne Versionsangabe gilt als Version 1")
    void missingVersionCountsAsOne() {
        GradeBookDto migrated = SchemaMigrator.migrate(fileOfVersion(0, null));

        assertEquals(GradeBookDto.CURRENT_SCHEMA_VERSION, migrated.schemaVersion());
        assertTrue(firstModule(migrated).plannedExams().isEmpty());
    }

    @Test
    @DisplayName("Version 2: geplante Prüfungen bleiben erhalten")
    void version2KeepsItsPlannedExams() {
        List<PlannedExamDto> planned = List.of(
                new PlannedExamDto("p1", "Test 2", new BigDecimal("2"), DATE.plusDays(30)));

        GradeBookDto migrated = SchemaMigrator.migrate(fileOfVersion(2, planned));

        assertEquals(1, firstModule(migrated).plannedExams().size());
        assertEquals("Test 2", firstModule(migrated).plannedExams().get(0).label());
        assertEquals(DATE.plusDays(30), firstModule(migrated).plannedExams().get(0).date());
    }

    @Test
    @DisplayName("Eine Datei aus einer neueren Programmversion wird abgelehnt")
    void refusesNewerVersions() {
        GradeBookDto newer = fileOfVersion(GradeBookDto.CURRENT_SCHEMA_VERSION + 1, null);

        assertThrows(RepositoryException.class, () -> SchemaMigrator.migrate(newer));
    }

    @Test
    @DisplayName("Ein leeres Notenbuch übersteht die Migration")
    void emptyFileSurvives() {
        GradeBookDto migrated = SchemaMigrator.migrate(new GradeBookDto(1, List.of()));

        assertEquals(GradeBookDto.CURRENT_SCHEMA_VERSION, migrated.schemaVersion());
        assertTrue(migrated.semesters().isEmpty());
    }
}
