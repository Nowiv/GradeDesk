package ch.notenrechner.persistence.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Serialisierungsform einer Note. Bewusst getrennt vom Modell: das Dateiformat
 * kann sich dadurch unabhängig von den JavaFX-Modellklassen weiterentwickeln.
 */
public record GradeDto(
        String id,
        String label,
        BigDecimal value,
        BigDecimal weight,
        LocalDate date) {
}
