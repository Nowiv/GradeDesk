package ch.gradedesk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Die Auswahl der nächsten Prüfung und das Verhältnis zwischen geplanten
 * Prüfungen und Berechnung.
 *
 * <p>«Heute» wird in allen Prüfungen von aussen gegeben. Ein Test, der auf die
 * Systemuhr schaut, ginge an einem beliebigen Tag im nächsten Jahr kaputt.</p>
 */
class PlannedExamTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    private static PlannedExam exam(String label, LocalDate date) {
        return new PlannedExam(label, BigDecimal.ONE, date);
    }

    @Nested
    @DisplayName("Nächste Prüfung")
    class NextExam {

        @Test
        @DisplayName("Ohne geplante Prüfung gibt es keine nächste")
        void withoutExamsThereIsNone() {
            assertTrue(new CourseModule("Mathematik").nextPlannedExam().isEmpty());
        }

        @Test
        @DisplayName("Die Prüfung mit dem frühesten Datum ist die nächste")
        void earliestDateWins() {
            CourseModule module = new CourseModule("Mathematik");
            PlannedExam late = exam("Schlussprüfung", TODAY.plusDays(30));
            PlannedExam soon = exam("Test 2", TODAY.plusDays(6));
            PlannedExam middle = exam("Test 3", TODAY.plusDays(12));
            module.getPlannedExams().addAll(List.of(late, soon, middle));

            assertSame(soon, module.nextPlannedExam().orElseThrow());
        }

        @Test
        @DisplayName("Eine überfällige Prüfung steht vor einer künftigen")
        void overdueComesFirst() {
            CourseModule module = new CourseModule("Mathematik");
            PlannedExam upcoming = exam("Test 2", TODAY.plusDays(3));
            PlannedExam overdue = exam("Test 1", TODAY.minusDays(4));
            module.getPlannedExams().addAll(List.of(upcoming, overdue));

            // Sie wartet auf ihre Note und darf nicht hinter dem spaeteren
            // Termin verschwinden.
            assertSame(overdue, module.nextPlannedExam().orElseThrow());
            assertTrue(overdue.isOverdue(TODAY));
            assertFalse(upcoming.isOverdue(TODAY));
        }

        @Test
        @DisplayName("Eine Prüfung von heute ist nicht überfällig")
        void todayIsNotOverdue() {
            assertFalse(exam("Test", TODAY).isOverdue(TODAY));
        }

        @Test
        @DisplayName("Bei zwei Prüfungen am selben Tag gilt die zuerst erfasste")
        void sameDayKeepsTheRecordedOrder() {
            CourseModule module = new CourseModule("Mathematik");
            PlannedExam first = exam("Vormittag", TODAY.plusDays(2));
            PlannedExam second = exam("Nachmittag", TODAY.plusDays(2));
            module.getPlannedExams().addAll(List.of(first, second));

            assertSame(first, module.nextPlannedExam().orElseThrow());
            assertEquals(List.of(first, second), module.plannedExamsByDate());
        }

        @Test
        @DisplayName("Sortiert wird nach Datum, unabhängig von der Erfassungsreihenfolge")
        void listIsSortedByDate() {
            CourseModule module = new CourseModule("Mathematik");
            PlannedExam late = exam("C", TODAY.plusDays(20));
            PlannedExam overdue = exam("A", TODAY.minusDays(1));
            PlannedExam soon = exam("B", TODAY.plusDays(2));
            module.getPlannedExams().addAll(List.of(late, overdue, soon));

            assertEquals(List.of(overdue, soon, late), module.plannedExamsByDate());
        }
    }

    @Nested
    @DisplayName("Keine Wirkung auf die Berechnung")
    class NoEffectOnCalculations {

        @Test
        @DisplayName("Ein Modul mit geplanter Prüfung, aber ohne Note, hat keine Modulnote")
        void plannedExamIsNoGrade() {
            CourseModule module = new CourseModule("Mathematik");
            module.getPlannedExams().add(exam("Test 1", TODAY.plusDays(5)));

            assertFalse(module.hasGrade());
            assertNull(module.getRoundedGrade());
            assertNull(module.getExactAverage());
        }

        @Test
        @DisplayName("Geplante Prüfungen verändern die Modulnote nicht")
        void moduleGradeStaysTheSame() {
            CourseModule module = new CourseModule("Mathematik");
            module.getGrades().add(new Grade("Prüfung 1", new BigDecimal("4.5"),
                    BigDecimal.ONE, TODAY.minusDays(10)));
            BigDecimal before = module.getRoundedGrade();
            BigDecimal exactBefore = module.getExactAverage();

            module.getPlannedExams().addAll(List.of(
                    exam("Test 2", TODAY.plusDays(5)),
                    new PlannedExam("Test 3", new BigDecimal("3"), TODAY.plusDays(9))));

            assertEquals(before, module.getRoundedGrade());
            assertEquals(exactBefore, module.getExactAverage());
        }
    }

    @Nested
    @DisplayName("Felder")
    class Fields {

        @Test
        @DisplayName("Ohne Bezeichnung ist der Text leer, nicht null")
        void labelDefaultsToEmpty() {
            PlannedExam exam = new PlannedExam(null, null, TODAY);

            assertEquals("", exam.getLabel());
            assertFalse(exam.hasLabel());
            assertEquals(0, exam.getWeight().compareTo(BigDecimal.ONE));
        }

        @Test
        @DisplayName("Eine Bezeichnung aus Leerzeichen zählt nicht als Bezeichnung")
        void blankLabelIsNoLabel() {
            assertFalse(new PlannedExam("   ", BigDecimal.ONE, TODAY).hasLabel());
        }
    }
}
