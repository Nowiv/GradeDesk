package ch.gradedesk.persistence.dto;

import java.util.List;

/**
 * Wurzelobjekt der Datei. Die Schema-Version erlaubt es, spätere
 * Formatänderungen zu erkennen und alte Dateien zu migrieren.
 */
public record GradeBookDto(
        int schemaVersion,
        List<SemesterDto> semesters) {

    /**
     * Aktuelle Version des Dateiformats.
     *
     * <p>Version 1: Semester, Module, Noten.<br>
     * Version 2: geplante Prüfungen je Modul.<br>
     * Version 3: abgeschlossene Semester.</p>
     */
    public static final int CURRENT_SCHEMA_VERSION = 3;

    public GradeBookDto {
        semesters = semesters == null ? List.of() : List.copyOf(semesters);
    }

    /** Ein leeres Notenbuch im aktuellen Format. */
    public static GradeBookDto empty() {
        return new GradeBookDto(CURRENT_SCHEMA_VERSION, List.of());
    }
}
