package ch.gradedesk.service;

import ch.gradedesk.model.CourseModule;
import ch.gradedesk.model.Grade;
import ch.gradedesk.model.GradeBook;
import ch.gradedesk.model.PlannedExam;
import ch.gradedesk.model.Semester;
import ch.gradedesk.persistence.GradeRepository;
import ch.gradedesk.persistence.LoadResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Fachliche Schnittstelle zwischen UI und Datenhaltung.
 *
 * <p>Jede Änderung läuft über diesen Service. Er validiert die Eingaben, pflegt
 * das Modell und kennt den Speicherort nur über das {@link GradeRepository}.
 * Das UI selbst enthält dadurch keine Fachlogik.</p>
 */
public final class GradeBookService {

    private final GradeBook gradeBook;
    private final GradeRepository repository;

    public GradeBookService(GradeBook gradeBook, GradeRepository repository) {
        this.gradeBook = Objects.requireNonNull(gradeBook, "gradeBook");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /** Das beobachtbare Modell, an das sich das UI bindet. */
    public GradeBook getGradeBook() {
        return gradeBook;
    }

    /** Lesbare Beschreibung des Speicherorts für die Einstellungen. */
    public String describeLocation() {
        return repository.describeLocation();
    }

    /**
     * Lädt den gespeicherten Stand in das Modell.
     *
     * @return das Ladeergebnis, damit das UI bei Auffälligkeiten informieren kann
     */
    public LoadResult load() {
        LoadResult result = repository.load();
        gradeBook.replaceAll(result.semesters());
        return result;
    }

    /** Schreibt den aktuellen Stand sofort. */
    public void save() {
        repository.save(gradeBook.getSemesters());
    }

    // ----- Semester -------------------------------------------------------

    /**
     * Legt ein Semester an.
     *
     * @throws ValidationException wenn der Name leer oder zu lang ist
     */
    public Semester addSemester(String name) {
        Semester semester = new Semester(GradeValidator.requireValidText(name));
        gradeBook.getSemesters().add(semester);
        return semester;
    }

    /**
     * Benennt ein Semester um.
     *
     * @throws ValidationException wenn der Name leer oder zu lang ist
     */
    public void renameSemester(Semester semester, String name) {
        semester.setName(GradeValidator.requireValidText(name));
    }

    /** Entfernt ein Semester samt Modulen und Noten. */
    public void removeSemester(Semester semester) {
        gradeBook.getSemesters().remove(semester);
    }

    // ----- Module ---------------------------------------------------------

    /**
     * Legt ein Modul innerhalb eines Semesters an.
     *
     * @throws ValidationException wenn der Name leer oder zu lang ist
     */
    public CourseModule addModule(Semester semester, String name) {
        Objects.requireNonNull(semester, "semester");
        CourseModule module = new CourseModule(GradeValidator.requireValidText(name));
        semester.getModules().add(module);
        return module;
    }

    /**
     * Benennt ein Modul um.
     *
     * @throws ValidationException wenn der Name leer oder zu lang ist
     */
    public void renameModule(CourseModule module, String name) {
        module.setName(GradeValidator.requireValidText(name));
    }

    /** Entfernt ein Modul samt Noten. */
    public void removeModule(Semester semester, CourseModule module) {
        semester.getModules().remove(module);
    }

    // ----- Noten ----------------------------------------------------------

    /**
     * Legt eine Note in einem Modul an.
     *
     * @throws ValidationException wenn eine Eingabe ungültig ist; das Modul bleibt dann unverändert
     */
    public Grade addGrade(CourseModule module, String label, BigDecimal value,
                          BigDecimal weight, LocalDate date) {
        Objects.requireNonNull(module, "module");
        Grade grade = new Grade(
                GradeValidator.requireValidText(label),
                GradeValidator.requireValidGrade(value),
                GradeValidator.requireValidWeight(weight),
                GradeValidator.requireValidDate(date));
        module.getGrades().add(grade);
        return grade;
    }

    /**
     * Ändert eine bestehende Note.
     *
     * @throws ValidationException wenn eine Eingabe ungültig ist; die Note bleibt dann unverändert
     */
    public void updateGrade(Grade grade, String label, BigDecimal value,
                            BigDecimal weight, LocalDate date) {
        Objects.requireNonNull(grade, "grade");
        grade.update(label, value, weight, date);
    }

    /** Entfernt eine Note. */
    public void removeGrade(CourseModule module, Grade grade) {
        module.getGrades().remove(grade);
    }

    // ----- Geplante Prüfungen ---------------------------------------------

    /**
     * Trägt eine geplante Prüfung in einem Modul ein.
     *
     * <p>Die Bezeichnung ist freiwillig, das Datum nicht. Geplante Prüfungen
     * verändern keine Berechnung.</p>
     *
     * @throws ValidationException wenn das Datum fehlt oder die Gewichtung nicht
     *                             grösser als 0 ist; das Modul bleibt dann unverändert
     */
    public PlannedExam addPlannedExam(CourseModule module, String label,
                                      BigDecimal weight, LocalDate date) {
        Objects.requireNonNull(module, "module");
        PlannedExam exam = new PlannedExam(
                GradeValidator.requireValidOptionalText(label),
                GradeValidator.requireValidWeight(weight),
                GradeValidator.requireValidDate(date));
        module.getPlannedExams().add(exam);
        return exam;
    }

    /**
     * Ändert eine geplante Prüfung.
     *
     * @throws ValidationException wenn eine Eingabe ungültig ist; der Eintrag
     *                             bleibt dann unverändert
     */
    public void updatePlannedExam(PlannedExam exam, String label,
                                  BigDecimal weight, LocalDate date) {
        Objects.requireNonNull(exam, "exam");
        exam.update(label, weight, date);
    }

    /** Entfernt eine geplante Prüfung. */
    public void removePlannedExam(CourseModule module, PlannedExam exam) {
        module.getPlannedExams().remove(exam);
    }

    /**
     * Macht aus einer geplanten Prüfung eine Note.
     *
     * <p>Erst wird die Note angelegt, dann verschwindet der geplante Eintrag.
     * Ist eine Eingabe ungültig, geschieht beides nicht: die geplante Prüfung
     * bleibt bestehen, damit sie nicht verloren geht.</p>
     *
     * @throws ValidationException wenn eine Eingabe ungültig ist
     */
    public Grade convertToGrade(CourseModule module, PlannedExam exam, String label,
                                BigDecimal value, BigDecimal weight, LocalDate date) {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(exam, "exam");
        Grade grade = addGrade(module, label, value, weight, date);
        module.getPlannedExams().remove(exam);
        return grade;
    }
}
