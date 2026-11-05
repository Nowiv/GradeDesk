package ch.notenrechner.service;

import java.math.BigDecimal;

/**
 * Die schweizerische Notenskala: 1.0 ist die schlechteste, 6.0 die beste Note,
 * ab 4.0 gilt eine Note als genügend.
 */
public final class GradeScale {

    /** Schlechteste zulässige Note. */
    public static final BigDecimal MIN = new BigDecimal("1.0");

    /** Beste zulässige Note. */
    public static final BigDecimal MAX = new BigDecimal("6.0");

    /** Ab dieser Note gilt eine Leistung als genügend. */
    public static final BigDecimal PASSING = new BigDecimal("4.0");

    /** Kleinste zulässige Gewichtung (exklusiv): eine Gewichtung muss grösser als 0 sein. */
    public static final BigDecimal MIN_WEIGHT_EXCLUSIVE = BigDecimal.ZERO;

    private GradeScale() {
    }

    /** Prüft, ob die Note innerhalb der Skala liegt. */
    public static boolean isInRange(BigDecimal grade) {
        return grade != null
                && grade.compareTo(MIN) >= 0
                && grade.compareTo(MAX) <= 0;
    }

    /** Prüft, ob die Note genügend ist (&gt;= 4.0). */
    public static boolean isPassing(BigDecimal grade) {
        return grade != null && grade.compareTo(PASSING) >= 0;
    }
}
