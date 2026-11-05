package ch.gradedesk.persistence.dto;

import java.util.List;

/** Serialisierungsform eines Moduls. */
public record ModuleDto(
        String id,
        String name,
        List<GradeDto> grades) {

    public ModuleDto {
        grades = grades == null ? List.of() : List.copyOf(grades);
    }
}
