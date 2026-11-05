package ch.gradedesk.ui.util;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.service.GradeScale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Einheitliche Darstellung von Noten, Gewichtungen und Daten im UI.
 *
 * <p>Hier steht nur Formatierung, keine Fachlogik. Gerechnet wird im Service.</p>
 *
 * <p>Noten behalten in beiden Sprachen den Punkt als Dezimaltrennzeichen: In der
 * Schweiz wird eine Note als «4.5» geschrieben, und eine Note, die je nach
 * Spracheinstellung anders aussieht, wäre beim Vergleichen mit dem Zeugnis nur
 * verwirrend. Sprachabhängig ist allein das Datum.</p>
 */
public final class Formats {

    private Formats() {
    }

    /** Platzhalter für «keine Note vorhanden». Ein Gedankenstrich, keine Null. */
    public static String none() {
        return I18n.t("value.none");
    }

    private static DateTimeFormatter dateFormatter() {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(I18n.getLanguage().locale());
    }

    /** Eine gerundete Note mit einer Dezimalstelle, z.B. {@code 4.5}, oder «–». */
    public static String grade(BigDecimal rounded) {
        return rounded == null ? none() : rounded.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    /** Ein Notenwert, wie er in ein Eingabefeld gehört: ohne überflüssige Nullen. */
    public static String gradeInput(BigDecimal value) {
        return value == null ? "" : stripZeros(value);
    }

    /** Ein ungerundeter Durchschnitt mit zwei Dezimalstellen, z.B. {@code 4.17}, oder leer. */
    public static String exact(BigDecimal exact) {
        return exact == null ? "" : exact.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Ein ungerundeter Durchschnitt als erklärender Satzteil, z.B. «ungerundet 4.17». */
    public static String exactLabel(BigDecimal exact) {
        return exact == null ? "" : I18n.t("value.exact", exact(exact));
    }

    /** Ein Durchschnitt mit vorangestelltem Mittelwertzeichen, z.B. «Ø 4.33». */
    public static String averageLabel(BigDecimal average) {
        return average == null ? none() : I18n.t("value.average", exact(average));
    }

    /** Eine Gewichtung ohne überflüssige Nullen, z.B. {@code 1}, {@code 1.5}, {@code 2}. */
    public static String weight(BigDecimal weight) {
        return weight == null ? none() : stripZeros(weight);
    }

    /** Eine Gewichtung als Faktor, z.B. {@code ×1.5}. */
    public static String weightFactor(BigDecimal weight) {
        return "×" + weight(weight);
    }

    private static String stripZeros(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }

    /** Ein Datum im Format der eingestellten Sprache. */
    public static String date(LocalDate date) {
        return date == null ? none() : dateFormatter().format(date);
    }

    /** «genügend», «ungenügend» oder «keine Note». */
    public static String statusText(BigDecimal rounded) {
        if (rounded == null) {
            return I18n.t("status.noGrade");
        }
        return I18n.t(GradeScale.isPassing(rounded) ? "status.pass" : "status.fail");
    }

    /** Die Stilklasse des Status-Abzeichens. */
    public static String statusBadgeClass(BigDecimal rounded) {
        if (rounded == null) {
            return "badge--none";
        }
        return GradeScale.isPassing(rounded) ? "badge--ok" : "badge--fail";
    }

    /**
     * Die Stilklasse zur Notenfarbe, z.B. {@code grade-fill--45}.
     *
     * @param prefix Präfix der Stilklasse, etwa {@code "grade-fill--"} für das
     *               Notenband oder {@code "bar--"} für eine Säule im Diagramm
     */
    public static String gradeColorClass(String prefix, BigDecimal rounded) {
        return prefix + gradeColorSuffix(rounded);
    }

    private static String gradeColorSuffix(BigDecimal rounded) {
        if (rounded == null || !GradeScale.isPassing(rounded)) {
            return "insufficient";
        }
        // Die Modulnote ist immer ein Vielfaches von 0.5, deshalb genügt ein Vergleich.
        if (rounded.compareTo(new BigDecimal("4.0")) == 0) {
            return "40";
        }
        if (rounded.compareTo(new BigDecimal("4.5")) == 0) {
            return "45";
        }
        if (rounded.compareTo(new BigDecimal("5.0")) == 0) {
            return "50";
        }
        if (rounded.compareTo(new BigDecimal("5.5")) == 0) {
            return "55";
        }
        return "60";
    }

    /** «1 Note» / «3 Noten» / «keine Noten», in der eingestellten Sprache. */
    public static String gradeCount(int count) {
        return I18n.t("count.grades", count);
    }

    /** «1 Modul» / «4 Module» / «keine Module». */
    public static String moduleCount(int count) {
        return I18n.t("count.modules", count);
    }

    /** «1 Semester» / «3 Semester». */
    public static String semesterCount(int count) {
        return I18n.t("count.semesters", count);
    }
}
