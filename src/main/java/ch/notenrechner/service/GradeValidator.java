package ch.notenrechner.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Prüft Benutzereingaben und liefert im Fehlerfall eine klare, direkt anzeigbare Meldung.
 * Die Prüfungen sind frei von UI-Code und damit ohne JavaFX testbar.
 */
public final class GradeValidator {

    /** Maximale Länge für Bezeichnungen und Namen. */
    public static final int MAX_TEXT_LENGTH = 80;

    private GradeValidator() {
    }

    /**
     * Prüft einen Notenwert gegen die Skala 1.0–6.0.
     *
     * @throws ValidationException wenn der Wert fehlt oder ausserhalb der Skala liegt
     */
    public static BigDecimal requireValidGrade(BigDecimal value) {
        if (value == null) {
            throw new ValidationException("Bitte eine Note zwischen 1.0 und 6.0 eingeben.");
        }
        if (!GradeScale.isInRange(value)) {
            throw new ValidationException("Die Note " + value.toPlainString()
                    + " liegt ausserhalb der Notenskala. Zulässig sind Werte von 1.0 bis 6.0.");
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
            throw new ValidationException("Bitte eine Gewichtung grösser als 0 eingeben.");
        }
        if (weight.compareTo(GradeScale.MIN_WEIGHT_EXCLUSIVE) <= 0) {
            throw new ValidationException("Die Gewichtung " + weight.toPlainString()
                    + " ist ungültig. Sie muss grösser als 0 sein.");
        }
        return weight;
    }

    /**
     * Prüft eine Bezeichnung (Notenname, Modulname, Semestername).
     *
     * @param text      die Eingabe
     * @param fieldName Feldname für die Fehlermeldung, z.B. {@code "Bezeichnung"}
     * @return der getrimmte Text
     * @throws ValidationException wenn der Text leer oder zu lang ist
     */
    public static String requireValidText(String text, String fieldName) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("Das Feld «" + fieldName + "» darf nicht leer sein.");
        }
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new ValidationException("Das Feld «" + fieldName + "» darf höchstens "
                    + MAX_TEXT_LENGTH + " Zeichen lang sein.");
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
            throw new ValidationException("Bitte ein Datum wählen.");
        }
        return date;
    }

    /**
     * Wandelt eine Benutzereingabe in eine Zahl. Punkt und Komma sind beide als
     * Dezimaltrennzeichen erlaubt.
     *
     * @param input     Rohtext aus einem Eingabefeld
     * @param fieldName Feldname für die Fehlermeldung
     * @throws ValidationException wenn der Text leer oder keine Zahl ist
     */
    public static BigDecimal parseDecimal(String input, String fieldName) {
        String normalized = input == null ? "" : input.trim().replace(',', '.');
        if (normalized.isEmpty()) {
            throw new ValidationException("Das Feld «" + fieldName + "» darf nicht leer sein.");
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new ValidationException("«" + input.trim() + "» ist keine gültige Zahl für das Feld «"
                    + fieldName + "».");
        }
    }
}
