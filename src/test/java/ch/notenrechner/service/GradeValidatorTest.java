package ch.notenrechner.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradeValidatorTest {

    @ParameterizedTest(name = "Note {0} ist zulässig")
    @ValueSource(strings = {"1.0", "1", "1.5", "3.9", "4.0", "4.25", "5.999", "6.0", "6"})
    void acceptsGradesInsideTheScale(String value) {
        assertDoesNotThrow(() -> GradeValidator.requireValidGrade(new BigDecimal(value)));
    }

    @ParameterizedTest(name = "Note {0} wird abgelehnt")
    @ValueSource(strings = {"0", "0.9", "0.99", "6.01", "7", "10", "-4"})
    void rejectsGradesOutsideTheScale(String value) {
        ValidationException thrown = assertThrows(ValidationException.class,
                () -> GradeValidator.requireValidGrade(new BigDecimal(value)));
        assertTrue(thrown.getMessage().contains("1.0 bis 6.0"),
                "Die Meldung soll die gültige Spanne nennen: " + thrown.getMessage());
    }

    @Test
    @DisplayName("Eine fehlende Note wird abgelehnt")
    void rejectsMissingGrade() {
        assertThrows(ValidationException.class, () -> GradeValidator.requireValidGrade(null));
    }

    @ParameterizedTest(name = "Gewichtung {0} ist zulässig")
    @ValueSource(strings = {"0.01", "0.5", "1", "1.0", "2", "10.5"})
    void acceptsPositiveWeights(String value) {
        assertDoesNotThrow(() -> GradeValidator.requireValidWeight(new BigDecimal(value)));
    }

    @ParameterizedTest(name = "Gewichtung {0} wird abgelehnt")
    @ValueSource(strings = {"0", "0.0", "-0.5", "-1"})
    void rejectsNonPositiveWeights(String value) {
        assertThrows(ValidationException.class,
                () -> GradeValidator.requireValidWeight(new BigDecimal(value)));
    }

    @Test
    @DisplayName("Eine fehlende Gewichtung wird abgelehnt")
    void rejectsMissingWeight() {
        assertThrows(ValidationException.class, () -> GradeValidator.requireValidWeight(null));
    }

    @Test
    @DisplayName("Text wird getrimmt und darf nicht leer sein")
    void validatesText() {
        assertEquals("Prüfung 1", GradeValidator.requireValidText("  Prüfung 1  ", "Bezeichnung"));
        assertThrows(ValidationException.class, () -> GradeValidator.requireValidText("   ", "Bezeichnung"));
        assertThrows(ValidationException.class, () -> GradeValidator.requireValidText(null, "Bezeichnung"));
        assertThrows(ValidationException.class, () -> GradeValidator.requireValidText(
                "x".repeat(GradeValidator.MAX_TEXT_LENGTH + 1), "Bezeichnung"));
        assertDoesNotThrow(() -> GradeValidator.requireValidText(
                "x".repeat(GradeValidator.MAX_TEXT_LENGTH), "Bezeichnung"));
    }

    @Test
    @DisplayName("Ein fehlendes Datum wird abgelehnt")
    void validatesDate() {
        LocalDate today = LocalDate.now();
        assertEquals(today, GradeValidator.requireValidDate(today));
        assertThrows(ValidationException.class, () -> GradeValidator.requireValidDate(null));
    }

    @Test
    @DisplayName("Punkt und Komma sind als Dezimaltrennzeichen erlaubt")
    void parsesBothDecimalSeparators() {
        assertEquals(new BigDecimal("4.5"), GradeValidator.parseDecimal("4.5", "Note"));
        assertEquals(new BigDecimal("4.5"), GradeValidator.parseDecimal("4,5", "Note"));
        assertEquals(new BigDecimal("4.5"), GradeValidator.parseDecimal("  4,5 ", "Note"));
    }

    @ParameterizedTest(name = "«{0}» ist keine Zahl")
    @ValueSource(strings = {"", "   ", "abc", "4.5.5", "4-5", "vier"})
    void rejectsNonNumericInput(String input) {
        assertThrows(ValidationException.class, () -> GradeValidator.parseDecimal(input, "Note"));
    }
}
