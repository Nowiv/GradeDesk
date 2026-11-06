package ch.gradedesk.persistence.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Serialisierungsform einer geplanten Prüfung.
 *
 * <p>Kein Notenwert: eine geplante Prüfung hat noch keinen. Sobald sie
 * geschrieben ist, wird sie zu einer {@link GradeDto} und verschwindet hier.</p>
 */
public record PlannedExamDto(
        String id,
        String label,
        BigDecimal weight,
        LocalDate date) {
}
