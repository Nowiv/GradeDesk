package ch.gradedesk.persistence.dto;

import java.util.List;

/**
 * Serialisierungsform eines Semesters.
 *
 * <p>{@code completed} ist ein {@code boolean}: fehlt das Feld in einer alten
 * Datei, steht dort {@code false} – ein Semester, von dem die Datei nichts
 * weiss, ist offen.</p>
 */
public record SemesterDto(
        String id,
        String name,
        boolean completed,
        List<ModuleDto> modules) {

    public SemesterDto {
        modules = modules == null ? List.of() : List.copyOf(modules);
    }
}
