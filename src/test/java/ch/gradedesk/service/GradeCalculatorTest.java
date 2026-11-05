package ch.gradedesk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradeCalculatorTest {

    /** Hilfstyp für die reine Rechenlogik, damit die Tests ohne Modellklassen auskommen. */
    private record Entry(BigDecimal value, BigDecimal weight) implements GradeCalculator.WeightedValue {
        static Entry of(String value, String weight) {
            return new Entry(new BigDecimal(value), new BigDecimal(weight));
        }

        static Entry of(String value) {
            return of(value, "1");
        }
    }

    @Nested
    @DisplayName("Kaufmännische Rundung auf 0.5")
    class Rounding {

        @ParameterizedTest(name = "{0} wird zu {1}")
        @CsvSource({
                // Grenzfälle aus der Spezifikation
                "4.24, 4.0",
                "4.25, 4.5",
                "4.74, 4.5",
                "4.75, 5.0",
                // weitere Grenzen
                "1.0,  1.0",
                "1.24, 1.0",
                "1.25, 1.5",
                "3.74, 3.5",
                "3.75, 4.0",
                "4.0,  4.0",
                "4.49, 4.5",
                "4.50, 4.5",
                "5.249, 5.0",
                "5.25, 5.5",
                "5.749, 5.5",
                "5.75, 6.0",
                "6.0,  6.0"
        })
        void roundsHalfUpToNearestHalfGrade(String input, String expected) {
            assertEquals(new BigDecimal(expected), GradeCalculator.roundToHalf(new BigDecimal(input)));
        }

        @Test
        @DisplayName("Das Ergebnis hat immer genau eine Dezimalstelle")
        void resultAlwaysHasOneDecimal() {
            assertEquals("4.0", GradeCalculator.roundToHalf(new BigDecimal("4")).toPlainString());
            assertEquals("5.5", GradeCalculator.roundToHalf(new BigDecimal("5.5000")).toPlainString());
        }

        @Test
        @DisplayName("Rundung bleibt auch bei Werten exakt, die als double Fehler erzeugen")
        void avoidsFloatingPointArtifacts() {
            // 0.1 + 0.2 wird als double zu 0.30000000000000004; mit BigDecimal bleibt es exakt.
            BigDecimal sum = new BigDecimal("4.05").add(new BigDecimal("0.2"));
            assertEquals(new BigDecimal("4.5"), GradeCalculator.roundToHalf(sum));
        }
    }

    @Nested
    @DisplayName("Gewichteter Durchschnitt")
    class WeightedAverage {

        @Test
        @DisplayName("Ohne Noten gibt es kein Ergebnis")
        void emptyWithoutGrades() {
            assertTrue(GradeCalculator.weightedAverage(List.of()).isEmpty());
            assertTrue(GradeCalculator.weightedAverage(null).isEmpty());
            assertTrue(GradeCalculator.moduleGrade(List.of()).isEmpty());
        }

        @Test
        @DisplayName("Eine einzelne Note ist der Durchschnitt")
        void singleGrade() {
            assertEquals(new BigDecimal("4.5000000000"),
                    GradeCalculator.weightedAverage(List.of(Entry.of("4.5"))).orElseThrow());
        }

        @Test
        @DisplayName("Gleiche Gewichtungen ergeben das arithmetische Mittel")
        void equalWeights() {
            Optional<BigDecimal> average = GradeCalculator.weightedAverage(
                    List.of(Entry.of("4.0"), Entry.of("5.0"), Entry.of("6.0")));
            assertEquals(0, average.orElseThrow().compareTo(new BigDecimal("5.0")));
        }

        @Test
        @DisplayName("Hoehere Gewichtung zieht den Durchschnitt staerker")
        void weightingShiftsAverage() {
            // 3.0 (Gewicht 2) und 6.0 (Gewicht 1) ergeben (6+6)/3 = 4.0
            Optional<BigDecimal> average = GradeCalculator.weightedAverage(
                    List.of(Entry.of("3.0", "2"), Entry.of("6.0", "1")));
            assertEquals(0, average.orElseThrow().compareTo(new BigDecimal("4.0")));
        }

        @Test
        @DisplayName("Dezimale Gewichtungen werden beruecksichtigt")
        void fractionalWeights() {
            // 5.0 (Gewicht 0.5) und 4.0 (Gewicht 1.5) ergeben (2.5+6)/2 = 4.25
            Optional<BigDecimal> average = GradeCalculator.weightedAverage(
                    List.of(Entry.of("5.0", "0.5"), Entry.of("4.0", "1.5")));
            assertEquals(0, average.orElseThrow().compareTo(new BigDecimal("4.25")));
            assertEquals(new BigDecimal("4.5"), GradeCalculator.moduleGrade(
                    List.of(Entry.of("5.0", "0.5"), Entry.of("4.0", "1.5"))).orElseThrow());
        }

        @Test
        @DisplayName("Nicht endender Dezimalbruch führt nicht zu einem Fehler")
        void handlesNonTerminatingDivision() {
            // (4 + 5 + 5) / 3 = 4.666...
            BigDecimal average = GradeCalculator.weightedAverage(
                    List.of(Entry.of("4.0"), Entry.of("5.0"), Entry.of("5.0"))).orElseThrow();
            assertEquals("4.6666666667", average.toPlainString());
            assertEquals(new BigDecimal("4.5"), GradeCalculator.roundToHalf(average));
        }

        @Test
        @DisplayName("Modulnote ist der gerundete gewichtete Durchschnitt")
        void moduleGradeRoundsTheWeightedAverage() {
            // (4.0*3 + 5.0*1) / 4 = 4.25 -> 4.5
            assertEquals(new BigDecimal("4.5"), GradeCalculator.moduleGrade(
                    List.of(Entry.of("4.0", "3"), Entry.of("5.0", "1"))).orElseThrow());
        }
    }

    @Nested
    @DisplayName("Semesterdurchschnitt")
    class SemesterAverage {

        @Test
        @DisplayName("Ohne Modulnoten gibt es kein Ergebnis")
        void emptyWithoutModuleGrades() {
            assertTrue(GradeCalculator.averageOfModuleGrades(List.of()).isEmpty());
            assertTrue(GradeCalculator.averageOfModuleGrades(null).isEmpty());
        }

        @Test
        @DisplayName("Mittel der gerundeten Modulnoten")
        void averagesRoundedModuleGrades() {
            BigDecimal average = GradeCalculator.averageOfModuleGrades(List.of(
                    new BigDecimal("4.0"), new BigDecimal("5.0"), new BigDecimal("5.5"))).orElseThrow();
            assertEquals(0, average.compareTo(new BigDecimal("4.8333333333")));
        }
    }
}
