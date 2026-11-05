package ch.notenrechner.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Reine Berechnungslogik für Noten. Bewusst ohne UI- und Modellabhängigkeit,
 * damit die Regeln isoliert testbar sind.
 *
 * <p>Alle Berechnungen laufen über {@link BigDecimal}, damit keine
 * Gleitkomma-Rundungsfehler entstehen.</p>
 */
public final class GradeCalculator {

    /** Interne Rechengenauigkeit für ungerundete Durchschnitte. */
    public static final int EXACT_SCALE = 10;

    private static final BigDecimal TWO = new BigDecimal("2");

    private GradeCalculator() {
    }

    /**
     * Kaufmännische Rundung auf die nächste halbe Note.
     * Beispiele: 4.24 → 4.0, 4.25 → 4.5, 4.74 → 4.5, 4.75 → 5.0.
     *
     * @param value ungerundeter Wert, nicht {@code null}
     * @return der gerundete Wert mit einer Dezimalstelle (z.B. {@code 4.5})
     */
    public static BigDecimal roundToHalf(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        return value.multiply(TWO)
                .setScale(0, RoundingMode.HALF_UP)
                .divide(TWO, 1, RoundingMode.UNNECESSARY);
    }

    /**
     * Gewichteter Durchschnitt, ungerundet.
     *
     * @param entries Wert/Gewichtung-Paare; leer oder {@code null} ergibt ein leeres Ergebnis
     * @return der ungerundete Durchschnitt oder {@link Optional#empty()}, wenn keine Werte vorliegen
     */
    public static Optional<BigDecimal> weightedAverage(Collection<? extends WeightedValue> entries) {
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightSum = BigDecimal.ZERO;
        for (WeightedValue entry : entries) {
            Objects.requireNonNull(entry, "entry");
            weightedSum = weightedSum.add(entry.value().multiply(entry.weight()));
            weightSum = weightSum.add(entry.weight());
        }
        if (weightSum.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(weightedSum.divide(weightSum, EXACT_SCALE, RoundingMode.HALF_UP));
    }

    /**
     * Modulnote: gewichteter Durchschnitt, kaufmännisch auf 0.5 gerundet.
     *
     * @return die gerundete Modulnote oder {@link Optional#empty()} bei einem Modul ohne Noten
     */
    public static Optional<BigDecimal> moduleGrade(Collection<? extends WeightedValue> entries) {
        return weightedAverage(entries).map(GradeCalculator::roundToHalf);
    }

    /**
     * Semesterdurchschnitt: ungewichteter Durchschnitt der bereits gerundeten Modulnoten.
     * Module ohne Note fliessen nicht ein und werden hier gar nicht übergeben.
     *
     * @param roundedModuleGrades gerundete Modulnoten
     * @return der Durchschnitt oder {@link Optional#empty()}, wenn kein Modul eine Note hat
     */
    public static Optional<BigDecimal> averageOfModuleGrades(Collection<BigDecimal> roundedModuleGrades) {
        if (roundedModuleGrades == null || roundedModuleGrades.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal grade : roundedModuleGrades) {
            sum = sum.add(Objects.requireNonNull(grade, "grade"));
        }
        return Optional.of(sum.divide(
                BigDecimal.valueOf(roundedModuleGrades.size()), EXACT_SCALE, RoundingMode.HALF_UP));
    }

    /** Ein zu gewichtender Notenwert. */
    public interface WeightedValue {

        /** Der Notenwert, nicht {@code null}. */
        BigDecimal value();

        /** Die Gewichtung, nicht {@code null} und grösser als 0. */
        BigDecimal weight();
    }
}
