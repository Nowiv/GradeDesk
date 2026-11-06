package ch.gradedesk.persistence;

import ch.gradedesk.model.CourseModule;
import ch.gradedesk.model.Grade;
import ch.gradedesk.model.PlannedExam;
import ch.gradedesk.model.Semester;
import ch.gradedesk.persistence.dto.GradeBookDto;
import ch.gradedesk.persistence.dto.GradeDto;
import ch.gradedesk.persistence.dto.ModuleDto;
import ch.gradedesk.persistence.dto.PlannedExamDto;
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
        List<PlannedExamDto> plannedExams = module.getPlannedExams().stream()
                .map(GradeBookMapper::toDto)
                .toList();
        return new ModuleDto(module.getId(), module.getName(), grades, plannedExams);
    }

    private static GradeDto toDto(Grade grade) {
        return new GradeDto(grade.getId(), grade.getLabel(), grade.getValue(),
                grade.getWeight(), grade.getDate());
    }

    private static PlannedExamDto toDto(PlannedExam exam) {
        return new PlannedExamDto(exam.getId(), exam.getLabel(),
                exam.getWeight(), exam.getDate());
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
        dto.plannedExams().stream()
                .filter(GradeBookMapper::isUsable)
                .map(GradeBookMapper::toModel)
                .forEach(module.getPlannedExams()::add);
        return module;
    }

    private static Grade toModel(GradeDto dto) {
        return new Grade(idOrNew(dto.id()), nameOrPlaceholder(dto.label(), "Note"),
                dto.value(), weightOrDefault(dto.weight()), dto.date());
    }

    private static PlannedExam toModel(PlannedExamDto dto) {
        return new PlannedExam(idOrNew(dto.id()), dto.label() == null ? "" : dto.label(),
                weightOrDefault(dto.weight()), dto.date());
    }

    /** Eine Note ohne Wert liesse sich nicht berechnen und wird deshalb verworfen. */
    private static boolean isUsable(GradeDto dto) {
        return dto != null && dto.value() != null;
    }

    /**
     * Eine geplante Prüfung ohne Datum liesse sich nirgends einordnen und wird
     * deshalb verworfen. Die Bezeichnung darf dagegen fehlen.
     */
    private static boolean isUsable(PlannedExamDto dto) {
        return dto != null && dto.date() != null;
    }

    private static BigDecimal weightOrDefault(BigDecimal weight) {
        return weight == null || weight.signum() <= 0 ? Grade.DEFAULT_WEIGHT : weight;
    }

    private static String idOrNew(String id) {
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }

    private static String nameOrPlaceholder(String name, String placeholder) {
        return name == null || name.isBlank() ? placeholder : name;
    }
}
