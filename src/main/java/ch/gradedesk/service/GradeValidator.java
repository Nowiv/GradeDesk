package ch.gradedesk.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Prüft Benutzereingaben.
 *
 * <p>Im Fehlerfall wird eine {@link ValidationException} mit einem Textschlüssel
 * geworfen; die Formulierung entsteht erst im UI. Dadurch sind die Regeln ohne
 * JavaFX und ohne Spracheinstellung testbar.</p>
 *
 * <p>Die Meldungen nennen das betroffene Feld nicht beim Namen: sie erscheinen
 * im UI direkt neben dem Feld, auf das sie sich beziehen. Dadurch bleibt die
 * Prüfung vollständig sprachfrei und ohne Wissen über die Oberfläche.</p>
 */
public final class GradeValidator {

    /** Maximale Länge für Bezeichnungen und Namen. */
    public static final int MAX_TEXT_LENGTH = 80;

    public static final String ERR_GRADE_MISSING = "validation.grade.missing";
    public static final String ERR_GRADE_OUT_OF_RANGE = "validation.grade.outOfRange";
    public static final String ERR_WEIGHT_MISSING = "validation.weight.missing";
    public static final String ERR_WEIGHT_NOT_POSITIVE = "validation.weight.notPositive";
    public static final String ERR_TEXT_EMPTY = "validation.text.empty";
    public static final String ERR_TEXT_TOO_LONG = "validation.text.tooLong";
    public static final String ERR_DATE_MISSING = "validation.date.missing";
    public static final String ERR_NUMBER_INVALID = "validation.number.invalid";

    private GradeValidator() {
    }

    /**
     * Prüft einen Notenwert gegen die Skala 1.0–6.0.
     *
     * @throws ValidationException wenn der Wert fehlt oder ausserhalb der Skala liegt
     */
    public static BigDecimal requireValidGrade(BigDecimal value) {
        if (value == null) {
            throw new ValidationException(ERR_GRADE_MISSING);
        }
        if (!GradeScale.isInRange(value)) {
            throw new ValidationException(ERR_GRADE_OUT_OF_RANGE,
                    value.toPlainString(), GradeScale.MIN.toPlainString(), GradeScale.MAX.toPlainString());
        }
        return value;
    }

    /**
     * Prüft eine Gewichtung.
     *
     * @throws ValidationException wenn die Gewichtung fehlt oder nicht grösser als 0 ist
     */
    public static BigDecimal requireValidWeight(BigDecimal weight) {
        if (weight == null) {
            throw new ValidationException(ERR_WEIGHT_MISSING);
        }
        if (weight.compareTo(GradeScale.MIN_WEIGHT_EXCLUSIVE) <= 0) {
            throw new ValidationException(ERR_WEIGHT_NOT_POSITIVE, weight.toPlainString());
        }
        return weight;
    }

    /**
     * Prüft eine Bezeichnung.
     *
     * @param text die Eingabe
     * @return der getrimmte Text
     * @throws ValidationException wenn der Text leer oder zu lang ist
     */
    public static String requireValidText(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(ERR_TEXT_EMPTY);
        }
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new ValidationException(ERR_TEXT_TOO_LONG, MAX_TEXT_LENGTH);
        }
        return trimmed;
    }

    /**
     * Prüft eine freiwillige Bezeichnung.
     *
     * <p>Leer ist hier ein gültiger Zustand: eine geplante Prüfung ist oft
     * zuerst nur ein Datum. Die Längenbeschränkung gilt trotzdem.</p>
     *
     * @param text die Eingabe, darf {@code null} oder leer sein
     * @return der getrimmte Text, möglicherweise leer
     * @throws ValidationException wenn der Text zu lang ist
     */
    public static String requireValidOptionalText(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new ValidationException(ERR_TEXT_TOO_LONG, MAX_TEXT_LENGTH);
        }
        return trimmed;
    }

    /**
     * Prüft ein Datum.
     *
     * @throws ValidationException wenn kein Datum gesetzt ist
     */
    public static LocalDate requireValidDate(LocalDate date) {
        if (date == null) {
            throw new ValidationException(ERR_DATE_MISSING);
        }
        return date;
    }

    /**
     * Wandelt eine Benutzereingabe in eine Zahl. Punkt und Komma sind beide als
     * Dezimaltrennzeichen erlaubt, weil beide Sprachen hier andere Gewohnheiten haben.
     *
     * @param input Rohtext aus einem Eingabefeld
     * @throws ValidationException wenn der Text leer oder keine Zahl ist
     */
    public static BigDecimal parseDecimal(String input) {
        String normalized = input == null ? "" : input.trim().replace(',', '.');
        if (normalized.isEmpty()) {
            throw new ValidationException(ERR_TEXT_EMPTY);
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new ValidationException(ERR_NUMBER_INVALID, input.trim());
        }
    }
}
