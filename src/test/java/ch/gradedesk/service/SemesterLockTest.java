package ch.gradedesk.service;

import ch.gradedesk.model.CourseModule;
import ch.gradedesk.model.Grade;
import ch.gradedesk.model.GradeBook;
import ch.gradedesk.model.PlannedExam;
import ch.gradedesk.model.Semester;
import ch.gradedesk.persistence.JsonGradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Was ein abgeschlossenes Semester sperrt – und was nicht.
 *
 * <p>Geprüft wird am Service, nicht am UI: ausgegraute Schaltflächen sind eine
 * Höflichkeit, die Sperre muss ohne sie halten.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SemesterLockTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 2);

    @TempDir
    Path dataDir;

    private GradeBookService service;
    private Semester semester;
    private CourseModule module;
    private Grade grade;
    private PlannedExam exam;

    @BeforeEach
    void setUp() {
        service = new GradeBookService(new GradeBook(), new JsonGradeRepository(dataDir));
        semester = service.addSemester("HS 2026");
        module = service.addModule(semester, "Mathematik");
        grade = service.addGrade(module, "Prüfung 1", new BigDecimal("4.5"),
                new BigDecimal("2"), DATE);
        exam = service.addPlannedExam(module, "Test 2", BigDecimal.ONE, DATE.plusDays(20));
    }

    /** Jede Operation, die ein abgeschlossenes Semester nicht mehr zulassen darf. */
    private Stream<Arguments> lockedOperations() {
        return Stream.of(
                Arguments.of("Semester umbenennen",
                        (Executable) () -> service.renameSemester(semester, "Neuer Name")),
                Arguments.of("Semester löschen",
                        (Executable) () -> service.removeSemester(semester)),
                Arguments.of("Modul anlegen",
                        (Executable) () -> service.addModule(semester, "Physik")),
                Arguments.of("Modul umbenennen",
                        (Executable) () -> service.renameModule(module, "Mathe")),
                Arguments.of("Modul löschen",
                        (Executable) () -> service.removeModule(semester, module)),
                Arguments.of("Note erfassen",
                        (Executable) () -> service.addGrade(module, "Prüfung 2",
                                new BigDecimal("5.0"), BigDecimal.ONE, DATE)),
                Arguments.of("Note ändern",
                        (Executable) () -> service.updateGrade(grade, "Prüfung 1",
                                new BigDecimal("6.0"), BigDecimal.ONE, DATE)),
                Arguments.of("Note löschen",
                        (Executable) () -> service.removeGrade(module, grade)),
                Arguments.of("Geplante Prüfung eintragen",
                        (Executable) () -> service.addPlannedExam(module, "Test 3",
                                BigDecimal.ONE, DATE.plusDays(30))),
                Arguments.of("Geplante Prüfung ändern",
                        (Executable) () -> service.updatePlannedExam(exam, "Test 2a",
                                BigDecimal.ONE, DATE.plusDays(21))),
                Arguments.of("Geplante Prüfung löschen",
                        (Executable) () -> service.removePlannedExam(module, exam)),
                Arguments.of("Geplante Prüfung in Note umwandeln",
                        (Executable) () -> service.convertToGrade(module, exam, "Test 2",
                                new BigDecimal("5.0"), BigDecimal.ONE, DATE.plusDays(20))));
    }

    @ParameterizedTest(name = "{0} ist gesperrt")
    @MethodSource("lockedOperations")
    @DisplayName("Jede ändernde Operation wird abgewiesen")
    void everyChangeIsRejected(String what, Executable operation) {
        service.completeSemester(semester);

        ValidationException thrown = assertThrows(ValidationException.class, operation, what);

        // Die Meldung nennt den Grund und das betroffene Semester, damit das UI
        // einen verständlichen Satz daraus machen kann.
        assertEquals(GradeValidator.ERR_SEMESTER_COMPLETED, thrown.getMessageKey());
        assertArrayEquals(new Object[] {"HS 2026"}, thrown.getArguments());
    }

    @ParameterizedTest(name = "{0} lässt die Daten unverändert")
    @MethodSource("lockedOperations")
    @DisplayName("Eine abgewiesene Änderung verändert nichts")
    void rejectedChangesLeaveTheDataAlone(String what, Executable operation) {
        service.completeSemester(semester);

        assertThrows(ValidationException.class, operation, what);

        assertEquals(1, service.getGradeBook().getSemesters().size());
        assertEquals("HS 2026", semester.getName());
        assertEquals(1, semester.getModules().size());
        assertEquals("Mathematik", module.getName());
        assertEquals(1, module.getGrades().size());
        assertEquals(0, grade.getValue().compareTo(new BigDecimal("4.5")));
        assertEquals(1, module.getPlannedExams().size());
        assertEquals("Test 2", exam.getLabel());
    }

    @Nested
    @DisplayName("Wieder öffnen")
    class Reopening {

        @Test
        @DisplayName("Nach dem Öffnen sind Änderungen wieder möglich")
        void changesWorkAgain() {
            service.completeSemester(semester);
            assertThrows(ValidationException.class,
                    () -> service.addGrade(module, "Prüfung 2", new BigDecimal("5.0"),
                            BigDecimal.ONE, DATE));

            service.reopenSemester(semester);

            assertFalse(semester.isCompleted());
            service.addGrade(module, "Prüfung 2", new BigDecimal("5.0"), BigDecimal.ONE, DATE);
            service.renameSemester(semester, "HS 2026 (korrigiert)");
            service.addPlannedExam(module, "Test 3", BigDecimal.ONE, DATE.plusDays(30));

            assertEquals(2, module.getGrades().size());
            assertEquals("HS 2026 (korrigiert)", semester.getName());
            assertEquals(2, module.getPlannedExams().size());
        }

        @Test
        @DisplayName("Abschliessen und Öffnen sind mehrfach möglich")
        void stampCanBeSetAndClearedRepeatedly() {
            service.completeSemester(semester);
            service.completeSemester(semester);
            assertTrue(semester.isCompleted());

            service.reopenSemester(semester);
            service.reopenSemester(semester);
            assertFalse(semester.isCompleted());
        }

        @Test
        @DisplayName("Ein offenes Semester lässt sich ohne Umweg ändern")
        void openSemesterIsNotLocked() {
            assertFalse(semester.isCompleted());

            service.renameSemester(semester, "FS 2027");

            assertEquals("FS 2027", semester.getName());
        }
    }

    @Nested
    @DisplayName("Nur die Sperre, nicht die Rechnung")
    class CalculationsAreUntouched {

        @Test
        @DisplayName("Berechnungen vor und nach dem Abschliessen sind identisch")
        void averagesStayTheSame() {
            service.addGrade(module, "Prüfung 2", new BigDecimal("3.5"), BigDecimal.ONE, DATE);
            CourseModule second = service.addModule(semester, "Englisch");
            service.addGrade(second, "Prüfung 1", new BigDecimal("5.5"), BigDecimal.ONE, DATE);

            BigDecimal moduleGrade = module.getRoundedGrade();
            BigDecimal exact = module.getExactAverage();
            BigDecimal semesterAverage = semester.getAverage();
            long gradedModules = semester.countGradedModules();

            service.completeSemester(semester);

            assertEquals(moduleGrade, module.getRoundedGrade());
            assertEquals(exact, module.getExactAverage());
            assertEquals(semesterAverage, semester.getAverage());
            assertEquals(gradedModules, semester.countGradedModules());

            service.reopenSemester(semester);

            assertEquals(moduleGrade, module.getRoundedGrade());
            assertEquals(semesterAverage, semester.getAverage());
        }

        @Test
        @DisplayName("Ein abgeschlossenes Semester zählt im Notenbuch normal mit")
        void closedSemesterStillCounts() {
            Semester open = service.addSemester("FS 2027");
            CourseModule openModule = service.addModule(open, "Physik");
            service.addGrade(openModule, "Prüfung 1", new BigDecimal("4.0"), BigDecimal.ONE, DATE);

            long modules = service.getGradeBook().countModules();
            long grades = service.getGradeBook().countGrades();
            long insufficient = service.getGradeBook().countInsufficientModules();

            service.completeSemester(semester);

            assertEquals(modules, service.getGradeBook().countModules());
            assertEquals(grades, service.getGradeBook().countGrades());
            assertEquals(insufficient, service.getGradeBook().countInsufficientModules());
            assertEquals(2, service.getGradeBook().getSemesters().size());
        }

        @Test
        @DisplayName("Ein abgeschlossenes Semester sperrt kein anderes")
        void otherSemestersStayOpen() {
            Semester other = service.addSemester("FS 2027");
            CourseModule otherModule = service.addModule(other, "Physik");

            service.completeSemester(semester);

            service.addGrade(otherModule, "Prüfung 1", new BigDecimal("4.0"), BigDecimal.ONE, DATE);
            service.renameSemester(other, "FS 2027 neu");

            assertEquals(1, otherModule.getGrades().size());
            assertEquals("FS 2027 neu", other.getName());
            assertTrue(semester.isCompleted());
            assertFalse(other.isCompleted());
        }
    }

    @Test
    @DisplayName("Ein neues Semester ist offen")
    void semestersStartOpen() {
        assertFalse(service.addSemester("FS 2027").isCompleted());
        assertTrue(service.getGradeBook().getSemesters().stream()
                .noneMatch(Semester::isCompleted));
    }
}
