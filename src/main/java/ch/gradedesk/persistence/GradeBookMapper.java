package ch.gradedesk.persistence;

import ch.gradedesk.model.CourseModule;
import ch.gradedesk.model.Grade;
import ch.gradedesk.model.Semester;
import ch.gradedesk.persistence.dto.GradeBookDto;
import ch.gradedesk.persistence.dto.GradeDto;
import ch.gradedesk.persistence.dto.ModuleDto;
import ch.gradedesk.persistence.dto.SemesterDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Übersetzt zwischen dem Laufzeitmodell und der Serialisierungsform.
 *
 * <p>Beim Lesen wird defensiv vorgegangen: fehlende Kennungen werden ergänzt und
 * unvollständige Einträge übersprungen, damit eine leicht beschädigte Datei nicht
 * den ganzen Datenbestand unbrauchbar macht.</p>
 */
public final class GradeBookMapper {

    private GradeBookMapper() {
    }

    /** Modell → Datei. */
    public static GradeBookDto toDto(List<Semester> semesters) {
        List<SemesterDto> semesterDtos = semesters.stream()
                .map(GradeBookMapper::toDto)
                .toList();
        return new GradeBookDto(GradeBookDto.CURRENT_SCHEMA_VERSION, semesterDtos);
    }

    private static SemesterDto toDto(Semester semester) {
        List<ModuleDto> modules = semester.getModules().stream()
                .map(GradeBookMapper::toDto)
                .toList();
        return new SemesterDto(semester.getId(), semester.getName(), modules);
    }

    private static ModuleDto toDto(CourseModule module) {
        List<GradeDto> grades = module.getGrades().stream()
                .map(GradeBookMapper::toDto)
                .toList();
        return new ModuleDto(module.getId(), module.getName(), grades);
    }

    private static GradeDto toDto(Grade grade) {
        return new GradeDto(grade.getId(), grade.getLabel(), grade.getValue(),
                grade.getWeight(), grade.getDate());
    }

    /** Datei → Modell. */
    public static List<Semester> toModel(GradeBookDto dto) {
        return dto.semesters().stream()
                .filter(semester -> semester != null)
                .map(GradeBookMapper::toModel)
                .toList();
    }

    private static Semester toModel(SemesterDto dto) {
        Semester semester = new Semester(idOrNew(dto.id()), nameOrPlaceholder(dto.name(), "Semester"));
        dto.modules().stream()
                .filter(module -> module != null)
                .map(GradeBookMapper::toModel)
                .forEach(semester.getModules()::add);
        return semester;
    }

    private static CourseModule toModel(ModuleDto dto) {
        CourseModule module = new CourseModule(idOrNew(dto.id()), nameOrPlaceholder(dto.name(), "Modul"));
        dto.grades().stream()
                .filter(GradeBookMapper::isUsable)
                .map(GradeBookMapper::toModel)
                .forEach(module.getGrades()::add);
        return module;
    }

    private static Grade toModel(GradeDto dto) {
        BigDecimal weight = dto.weight() == null || dto.weight().signum() <= 0
                ? Grade.DEFAULT_WEIGHT
                : dto.weight();
        return new Grade(idOrNew(dto.id()), nameOrPlaceholder(dto.label(), "Note"),
                dto.value(), weight, dto.date());
    }

    /** Eine Note ohne Wert liesse sich nicht berechnen und wird deshalb verworfen. */
    private static boolean isUsable(GradeDto dto) {
        return dto != null && dto.value() != null;
    }

    private static String idOrNew(String id) {
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }

    private static String nameOrPlaceholder(String name, String placeholder) {
        return name == null || name.isBlank() ? placeholder : name;
    }
}
