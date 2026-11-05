package ch.notenrechner.ui.util;

import ch.notenrechner.service.GradeScale;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Einheitliche Darstellung von Noten, Gewichtungen und Daten im UI.
 *
 * <p>Hier steht nur Formatierung, keine Fachlogik. Gerechnet wird im Service.</p>
 */
public final class Formats {

    /** Platzhalter für «keine Note vorhanden». Ein Gedankenstrich, keine Null. */
    public static final String NO_VALUE = "–";

    private static final Locale CH = Locale.forLanguageTag("de-CH");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", CH);

    private Formats() {
    }

    /** Eine gerundete Note mit einer Dezimalstelle, z.B. {@code 4.5}, oder «–». */
    public static String grade(BigDecimal rounded) {
        return rounded == null ? NO_VALUE : rounded.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    /** Ein ungerundeter Durchschnitt mit zwei Dezimalstellen, z.B. {@code 4.17}, oder leer. */
    public static String exact(BigDecimal exact) {
        return exact == null ? "" : exact.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Ein ungerundeter Durchschnitt als erklärender Satzteil, z.B. «ungerundet 4.17». */
    public static String exactLabel(BigDecimal exact) {
        return exact == null ? "" : "ungerundet " + exact(exact);
    }

    /** Ein Notenwert so, wie er in ein Eingabefeld gehört: ohne überflüssige Nullen. */
    public static String gradeInput(BigDecimal value) {
        if (value == null) {
            return "";
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }

    /** Eine Gewichtung ohne überflüssige Nullen, z.B. {@code 1}, {@code 1.5}, {@code 2}. */
    public static String weight(BigDecimal weight) {
        if (weight == null) {
            return NO_VALUE;
        }
        BigDecimal stripped = weight.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }

    /** Eine Gewichtung als Faktor, z.B. {@code ×1.5}. */
    public static String weightFactor(BigDecimal weight) {
        return "×" + weight(weight);
    }

    /** Ein Datum im Schweizer Format, z.B. {@code 12.09.2025}. */
    public static String date(LocalDate date) {
        return date == null ? NO_VALUE : DATE.format(date);
    }

    /** «genügend», «ungenügend» oder «keine Note». */
    public static String statusText(BigDecimal rounded) {
        if (rounded == null) {
            return "keine Note";
        }
        return GradeScale.isPassing(rounded) ? "genügend" : "ungenügend";
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

    /** «1 Note» / «3 Noten» / «keine Noten». */
    public static String gradeCount(int count) {
        return switch (count) {
            case 0 -> "keine Noten";
            case 1 -> "1 Note";
            default -> count + " Noten";
        };
    }

    /** «1 Modul» / «4 Module» / «keine Module». */
    public static String moduleCount(int count) {
        return switch (count) {
            case 0 -> "keine Module";
            case 1 -> "1 Modul";
            default -> count + " Module";
        };
    }
}
