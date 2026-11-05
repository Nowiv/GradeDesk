package ch.gradedesk.persistence.dto;

import java.util.List;

/** Serialisierungsform eines Semesters. */
public record SemesterDto(
        String id,
        String name,
        List<ModuleDto> modules) {

    public SemesterDto {
        modules = modules == null ? List.of() : List.copyOf(modules);
    }
}
