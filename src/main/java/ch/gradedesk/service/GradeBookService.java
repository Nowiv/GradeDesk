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
import java.util.function.Predicate;

/**
 * Fachliche Schnittstelle zwischen UI und Datenhaltung.
 *
 * <p>Jede Änderung läuft über diesen Service. Er validiert die Eingaben, pflegt
 * das Modell und kennt den Speicherort nur über das {@link GradeRepository}.
 * Das UI selbst enthält dadurch keine Fachlogik.</p>
 *
 * <p>Hier liegt auch die Sperre abgeschlossener Semester. Sie gilt für jede
 * ändernde Operation, gleich von wo sie kommt: ausgegraute Schaltflächen sind
 * ein Hinweis für die Person, die Zusicherung gibt erst diese Stelle.</p>
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
        requireOpen(semester);
        semester.setName(GradeValidator.requireValidText(name));
    }

    /**
     * Entfernt ein Semester samt Modulen und Noten.
     *
     * @throws ValidationException wenn das Semester abgeschlossen ist
     */
    public void removeSemester(Semester semester) {
        requireOpen(semester);
        gradeBook.getSemesters().remove(semester);
    }

    /**
     * Stempelt ein Semester als abgeschlossen.
     *
     * <p>Danach lässt sich daran nichts mehr ändern, bis es wieder geöffnet
     * wird. An den Berechnungen ändert sich nichts.</p>
     */
    public void completeSemester(Semester semester) {
        Objects.requireNonNull(semester, "semester");
        semester.setCompleted(true);
    }

    /**
     * Öffnet ein abgeschlossenes Semester wieder.
     *
     * <p>Bewusst ohne Prüfung des bisherigen Zustands: ein zweites Öffnen eines
     * offenen Semesters ist kein Fehler, es passiert nur nichts.</p>
     */
    public void reopenSemester(Semester semester) {
        Objects.requireNonNull(semester, "semester");
        semester.setCompleted(false);
    }

    // ----- Module ---------------------------------------------------------

    /**
     * Legt ein Modul innerhalb eines Semesters an.
     *
     * @throws ValidationException wenn der Name leer oder zu lang ist
     */
    public CourseModule addModule(Semester semester, String name) {
        Objects.requireNonNull(semester, "semester");
        requireOpen(semester);
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
        requireOpenFor(module);
        module.setName(GradeValidator.requireValidText(name));
    }

    /**
     * Entfernt ein Modul samt Noten.
     *
     * @throws ValidationException wenn das Semester abgeschlossen ist
     */
    public void removeModule(Semester semester, CourseModule module) {
        requireOpen(semester);
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
        requireOpenFor(module);
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
        requireOpenFor(moduleOf(grade));
        grade.update(label, value, weight, date);
    }

    /**
     * Entfernt eine Note.
     *
     * @throws ValidationException wenn das Semester abgeschlossen ist
     */
    public void removeGrade(CourseModule module, Grade grade) {
        requireOpenFor(module);
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
        requireOpenFor(module);
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
        requireOpenFor(moduleOf(exam));
        exam.update(label, weight, date);
    }

    /**
     * Entfernt eine geplante Prüfung.
     *
     * @throws ValidationException wenn das Semester abgeschlossen ist
     */
    public void removePlannedExam(CourseModule module, PlannedExam exam) {
        requireOpenFor(module);
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
        requireOpenFor(module);
        Grade grade = addGrade(module, label, value, weight, date);
        module.getPlannedExams().remove(exam);
        return grade;
    }

    // ----- Sperre abgeschlossener Semester ---------------------------------

    /**
     * Lässt eine Änderung nur zu, solange das Semester offen ist.
     *
     * @throws ValidationException mit einem Textschlüssel, den das UI in der
     *                             eingestellten Sprache formuliert
     */
    private void requireOpen(Semester semester) {
        if (semester != null && semester.isCompleted()) {
            throw new ValidationException(
                    GradeValidator.ERR_SEMESTER_COMPLETED, semester.getName());
        }
    }

    private void requireOpenFor(CourseModule module) {
        requireOpen(module == null ? null : semesterOf(module));
    }

    /**
     * Sucht das Semester, zu dem ein Modul gehört.
     *
     * <p>Gesucht statt im Modul vermerkt: ein Rückverweis vom Kind auf den
     * Elternteil müsste bei jeder Änderung nachgeführt werden, und das
     * Notenbuch einer einzelnen Person ist klein genug, dass die Suche nicht
     * ins Gewicht fällt.</p>
     *
     * @return das Semester oder {@code null}, wenn das Modul nirgends hängt
     */
    private Semester semesterOf(CourseModule module) {
        return gradeBook.getSemesters().stream()
                .filter(semester -> semester.getModules().contains(module))
                .findFirst()
                .orElse(null);
    }

    private CourseModule moduleOf(Grade grade) {
        return findModule(module -> module.getGrades().contains(grade));
    }

    private CourseModule moduleOf(PlannedExam exam) {
        return findModule(module -> module.getPlannedExams().contains(exam));
    }

    private CourseModule findModule(Predicate<CourseModule> match) {
        return gradeBook.getSemesters().stream()
                .flatMap(semester -> semester.getModules().stream())
                .filter(match)
                .findFirst()
                .orElse(null);
    }
}
