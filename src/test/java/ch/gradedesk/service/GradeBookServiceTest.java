package ch.gradedesk.service;

import ch.gradedesk.model.CourseModule;
import ch.gradedesk.model.Grade;
import ch.gradedesk.model.GradeBook;
import ch.gradedesk.model.PlannedExam;
import ch.gradedesk.model.Semester;
import ch.gradedesk.persistence.JsonGradeRepository;
import ch.gradedesk.persistence.LoadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradeBookServiceTest {

    private static final LocalDate DATE = LocalDate.of(2025, 10, 1);

    @TempDir
    Path dataDir;

    private GradeBookService service;

    @BeforeEach
    void setUp() {
        service = new GradeBookService(new GradeBook(), new JsonGradeRepository(dataDir));
    }

    @Test
    @DisplayName("Semester, Module und Noten lassen sich anlegen")
    void createsTheHierarchy() {
        Semester semester = service.addSemester("HS 2025");
        CourseModule module = service.addModule(semester, "Mathematik");
        service.addGrade(module, "Prüfung 1", new BigDecimal("5.5"), BigDecimal.ONE, DATE);

        assertEquals(1, service.getGradeBook().getSemesters().size());
        assertEquals(1, semester.getModules().size());
        assertEquals(1, module.getGrades().size());
        assertEquals(new BigDecimal("5.5"), module.getRoundedGrade());
    }

    @Test
    @DisplayName("Ein Modul ohne Noten hat keine Modulnote und zählt nicht mit")
    void modulesWithoutGradesAreIgnored() {
        Semester semester = service.addSemester("HS 2025");
        CourseModule withGrade = service.addModule(semester, "Mathematik");
        service.addModule(semester, "Sport");
        service.addGrade(withGrade, "Prüfung 1", new BigDecimal("5.0"), BigDecimal.ONE, DATE);

        assertNull(semester.getModules().get(1).getRoundedGrade());
        // Der Semesterschnitt beruht nur auf dem Modul mit Note.
        assertEquals(0, semester.getAverage().compareTo(new BigDecimal("5.0")));
    }

    @Test
    @DisplayName("Eine Note ausserhalb von 1.0-6.0 wird abgelehnt und nichts verändert")
    void rejectsGradesOutsideTheScale() {
        Semester semester = service.addSemester("HS 2025");
        CourseModule module = service.addModule(semester, "Mathematik");

        assertThrows(ValidationException.class, () -> service.addGrade(
                module, "Zu gut", new BigDecimal("6.5"), BigDecimal.ONE, DATE));
        assertThrows(ValidationException.class, () -> service.addGrade(
                module, "Zu schlecht", new BigDecimal("0.5"), BigDecimal.ONE, DATE));

        assertTrue(module.getGrades().isEmpty(), "Eine abgelehnte Eingabe darf nichts anlegen");
    }

    @Test
    @DisplayName("Eine Gewichtung von 0 oder weniger wird abgelehnt")
    void rejectsNonPositiveWeight() {
        Semester semester = service.addSemester("HS 2025");
        CourseModule module = service.addModule(semester, "Mathematik");

        assertThrows(ValidationException.class, () -> service.addGrade(
                module, "Ohne Gewicht", new BigDecimal("4.0"), BigDecimal.ZERO, DATE));
        assertTrue(module.getGrades().isEmpty());
    }

    @Test
    @DisplayName("Leere Namen werden abgelehnt")
    void rejectsBlankNames() {
        assertThrows(ValidationException.class, () -> service.addSemester("  "));
        Semester semester = service.addSemester("HS 2025");
        assertThrows(ValidationException.class, () -> service.addModule(semester, ""));
        assertTrue(semester.getModules().isEmpty());
    }

    @Test
    @DisplayName("Eine ungültige Änderung lässt die bestehende Note unverändert")
    void failedUpdateLeavesGradeUntouched() {
        Semester semester = service.addSemester("HS 2025");
        CourseModule module = service.addModule(semester, "Mathematik");
        Grade grade = service.addGrade(module, "Prüfung 1", new BigDecimal("5.0"), BigDecimal.ONE, DATE);

        assertThrows(ValidationException.class, () -> service.updateGrade(
                grade, "Prüfung 1", new BigDecimal("7.0"), BigDecimal.ONE, DATE));

        assertEquals(new BigDecimal("5.0"), grade.getValue());
        assertEquals("Prüfung 1", grade.getLabel());
    }

    @Test
    @DisplayName("Löschen entfernt die Daten vollständig")
    void deletesData() {
        Semester semester = service.addSemester("HS 2025");
        CourseModule module = service.addModule(semester, "Mathematik");
        Grade grade = service.addGrade(module, "Prüfung 1", new BigDecimal("4.0"), BigDecimal.ONE, DATE);

        service.removeGrade(module, grade);
        assertTrue(module.getGrades().isEmpty());

        service.removeModule(semester, module);
        assertTrue(semester.getModules().isEmpty());

        service.removeSemester(semester);
        assertTrue(service.getGradeBook().getSemesters().isEmpty());
    }

    @Test
    @DisplayName("Gespeicherter Stand wird beim Laden wiederhergestellt")
    void savesAndLoadsThroughTheRepository() {
        Semester semester = service.addSemester("HS 2025");
        CourseModule module = service.addModule(semester, "Mathematik");
        service.addGrade(module, "Prüfung 1", new BigDecimal("4.0"), new BigDecimal("3"), DATE);
        service.addGrade(module, "Prüfung 2", new BigDecimal("5.0"), BigDecimal.ONE, DATE);
        service.save();

        GradeBookService reopened =
                new GradeBookService(new GradeBook(), new JsonGradeRepository(dataDir));
        LoadResult result = reopened.load();

        assertEquals(LoadResult.Status.LOADED, result.status());
        CourseModule reloaded = reopened.getGradeBook().getSemesters().get(0).getModules().get(0);
        // (4.0*3 + 5.0*1) / 4 = 4.25 -> 4.5
        assertEquals(new BigDecimal("4.5"), reloaded.getRoundedGrade());
        assertEquals(0, reloaded.getExactAverage().compareTo(new BigDecimal("4.25")));
    }

    @Test
    @DisplayName("Automatisches Speichern schreibt jede Änderung")
    void autoSaveWritesEveryChange() {
        try (AutoSaveService autoSave = new AutoSaveService(service, Runnable::run, 0)) {
            autoSave.start();
            assertEquals(AutoSaveService.SaveState.IDLE, autoSave.getState());

            Semester semester = service.addSemester("HS 2025");
            assertEquals(AutoSaveService.SaveState.SAVED, autoSave.getState());

            CourseModule module = service.addModule(semester, "Mathematik");
            service.addGrade(module, "Prüfung 1", new BigDecimal("5.5"), BigDecimal.ONE, DATE);
            assertEquals(AutoSaveService.SaveState.SAVED, autoSave.getState());

            GradeBookService reopened =
                    new GradeBookService(new GradeBook(), new JsonGradeRepository(dataDir));
            reopened.load();
            assertEquals(new BigDecimal("5.5"), reopened.getGradeBook()
                    .getSemesters().get(0).getModules().get(0).getRoundedGrade());
        }
    }

    @Test
    @DisplayName("Automatisches Speichern erfasst auch das Ändern einer bestehenden Note")
    void autoSaveReactsToNestedChanges() {
        Semester semester = service.addSemester("HS 2025");
        CourseModule module = service.addModule(semester, "Mathematik");
        Grade grade = service.addGrade(module, "Prüfung 1", new BigDecimal("4.0"), BigDecimal.ONE, DATE);

        try (AutoSaveService autoSave = new AutoSaveService(service, Runnable::run, 0)) {
            autoSave.start();
            service.updateGrade(grade, "Prüfung 1", new BigDecimal("6.0"), BigDecimal.ONE, DATE);

            assertEquals(AutoSaveService.SaveState.SAVED, autoSave.getState());
        }

        GradeBookService reopened =
                new GradeBookService(new GradeBook(), new JsonGradeRepository(dataDir));
        reopened.load();
        assertEquals(new BigDecimal("6.0"), reopened.getGradeBook()
                .getSemesters().get(0).getModules().get(0).getRoundedGrade());
    }

    // ----- Geplante Prüfungen ----------------------------------------------

    @Test
    @DisplayName("Eine geplante Prüfung lässt sich eintragen, ändern und löschen")
    void managesPlannedExams() {
        CourseModule module = service.addModule(service.addSemester("HS 2025"), "Mathematik");

        PlannedExam exam = service.addPlannedExam(module, "Test 1", BigDecimal.ONE, DATE);
        assertEquals(1, module.getPlannedExams().size());
        assertEquals("Test 1", exam.getLabel());

        service.updatePlannedExam(exam, "Test 1a", new BigDecimal("2"), DATE.plusDays(7));
        assertEquals("Test 1a", exam.getLabel());
        assertEquals(0, exam.getWeight().compareTo(new BigDecimal("2")));
        assertEquals(DATE.plusDays(7), exam.getDate());

        service.removePlannedExam(module, exam);
        assertTrue(module.getPlannedExams().isEmpty());
    }

    @Test
    @DisplayName("Die Bezeichnung einer geplanten Prüfung ist freiwillig")
    void plannedExamLabelIsOptional() {
        CourseModule module = service.addModule(service.addSemester("HS 2025"), "Mathematik");

        PlannedExam exam = service.addPlannedExam(module, "   ", BigDecimal.ONE, DATE);

        assertEquals("", exam.getLabel());
        assertFalse(exam.hasLabel());
    }

    @Test
    @DisplayName("Eine geplante Prüfung ohne Datum wird abgelehnt")
    void plannedExamNeedsADate() {
        CourseModule module = service.addModule(service.addSemester("HS 2025"), "Mathematik");

        assertThrows(ValidationException.class,
                () -> service.addPlannedExam(module, "Test 1", BigDecimal.ONE, null));
        assertTrue(module.getPlannedExams().isEmpty());
    }

    @Test
    @DisplayName("Eine Gewichtung von 0 oder weniger wird abgelehnt")
    void plannedExamNeedsAPositiveWeight() {
        CourseModule module = service.addModule(service.addSemester("HS 2025"), "Mathematik");

        assertThrows(ValidationException.class,
                () -> service.addPlannedExam(module, "Test 1", BigDecimal.ZERO, DATE));
        assertThrows(ValidationException.class,
                () -> service.addPlannedExam(module, "Test 1", new BigDecimal("-1"), DATE));
        assertTrue(module.getPlannedExams().isEmpty());
    }

    @Test
    @DisplayName("Eine ungültige Änderung lässt die geplante Prüfung unverändert")
    void failedUpdateLeavesThePlannedExamAlone() {
        CourseModule module = service.addModule(service.addSemester("HS 2025"), "Mathematik");
        PlannedExam exam = service.addPlannedExam(module, "Test 1", BigDecimal.ONE, DATE);

        assertThrows(ValidationException.class,
                () -> service.updatePlannedExam(exam, "Test 2", BigDecimal.ZERO, DATE.plusDays(1)));

        assertEquals("Test 1", exam.getLabel());
        assertEquals(DATE, exam.getDate());
        assertEquals(0, exam.getWeight().compareTo(BigDecimal.ONE));
    }

    @Test
    @DisplayName("Aus einer geplanten Prüfung wird eine Note, der Eintrag verschwindet")
    void turnsAPlannedExamIntoAGrade() {
        CourseModule module = service.addModule(service.addSemester("HS 2025"), "Mathematik");
        PlannedExam exam = service.addPlannedExam(module, "Test 1", new BigDecimal("2"), DATE);

        Grade grade = service.convertToGrade(module, exam, exam.getLabel(),
                new BigDecimal("5.0"), exam.getWeight(), exam.getDate());

        assertTrue(module.getPlannedExams().isEmpty());
        assertEquals(1, module.getGrades().size());
        assertEquals("Test 1", grade.getLabel());
        assertEquals(0, grade.getWeight().compareTo(new BigDecimal("2")));
        assertEquals(DATE, grade.getDate());
        assertEquals(new BigDecimal("5.0"), module.getRoundedGrade());
    }

    @Test
    @DisplayName("Scheitert die Note, bleibt die geplante Prüfung bestehen")
    void keepsThePlannedExamWhenTheGradeIsInvalid() {
        CourseModule module = service.addModule(service.addSemester("HS 2025"), "Mathematik");
        PlannedExam exam = service.addPlannedExam(module, "Test 1", BigDecimal.ONE, DATE);

        assertThrows(ValidationException.class, () -> service.convertToGrade(
                module, exam, "Test 1", new BigDecimal("7.0"), BigDecimal.ONE, DATE));

        assertEquals(1, module.getPlannedExams().size());
        assertTrue(module.getGrades().isEmpty());
    }

    @Test
    @DisplayName("Durchschnitte sind mit und ohne geplante Prüfungen identisch")
    void plannedExamsDoNotChangeAnyAverage() {
        Semester semester = service.addSemester("HS 2025");
        CourseModule math = service.addModule(semester, "Mathematik");
        CourseModule sport = service.addModule(semester, "Sport");
        service.addGrade(math, "Prüfung 1", new BigDecimal("4.5"), new BigDecimal("2"), DATE);
        service.addGrade(math, "Prüfung 2", new BigDecimal("3.5"), BigDecimal.ONE, DATE);

        BigDecimal moduleGrade = math.getRoundedGrade();
        BigDecimal exact = math.getExactAverage();
        BigDecimal semesterAverage = semester.getAverage();

        service.addPlannedExam(math, "Test 3", new BigDecimal("3"), DATE.plusDays(14));
        service.addPlannedExam(sport, "Lauftest", BigDecimal.ONE, DATE.plusDays(2));

        assertEquals(moduleGrade, math.getRoundedGrade());
        assertEquals(exact, math.getExactAverage());
        assertEquals(semesterAverage, semester.getAverage());
        // Ein Modul mit nur einer geplanten Prüfung bleibt ohne Note.
        assertFalse(sport.hasGrade());
    }

    @Test
    @DisplayName("Automatisches Speichern erfasst auch geplante Prüfungen")
    void autoSaveCoversPlannedExams() {
        CourseModule module = service.addModule(service.addSemester("HS 2025"), "Mathematik");

        try (AutoSaveService autoSave = new AutoSaveService(service, Runnable::run, 0)) {
            autoSave.start();
            service.addPlannedExam(module, "Test 1", BigDecimal.ONE, DATE);

            assertEquals(AutoSaveService.SaveState.SAVED, autoSave.getState());
        }

        GradeBookService reopened =
                new GradeBookService(new GradeBook(), new JsonGradeRepository(dataDir));
        reopened.load();
        CourseModule loaded = reopened.getGradeBook().getSemesters()
                .get(0).getModules().get(0);
        assertEquals(1, loaded.getPlannedExams().size());
        assertEquals("Test 1", loaded.getPlannedExams().get(0).getLabel());
    }
}
