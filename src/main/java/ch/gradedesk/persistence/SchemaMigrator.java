package ch.gradedesk.persistence;

import ch.gradedesk.persistence.dto.GradeBookDto;
import ch.gradedesk.persistence.dto.ModuleDto;
import ch.gradedesk.persistence.dto.SemesterDto;

import java.util.List;

/**
 * Bringt eine geladene Datei auf das aktuelle Dateiformat.
 *
 * <p>Erweiterungspunkt für künftige Formatänderungen: pro neuer Version kommt ein
 * weiterer Schritt hinzu, der die Daten von der Vorgängerversion übersetzt. Dateien
 * einer neueren Version als der Anwendung werden bewusst nicht geöffnet, damit eine
 * ältere Programmversion keine Daten verliert.</p>
 *
 * <p>Die Schritte laufen nacheinander: eine Datei der Version 1 geht durch jeden
 * Schritt bis zur aktuellen Version, nicht direkt ans Ziel. So bleibt jeder
 * Schritt für sich klein und prüfbar.</p>
 */
public final class SchemaMigrator {

    private SchemaMigrator() {
    }

    /**
     * @param dto die gelesene Datei
     * @return der Inhalt im aktuellen Format
     * @throws RepositoryException wenn die Datei von einer neueren Programmversion stammt
     */
    public static GradeBookDto migrate(GradeBookDto dto) {
        int version = dto.schemaVersion() <= 0 ? 1 : dto.schemaVersion();

        if (version > GradeBookDto.CURRENT_SCHEMA_VERSION) {
            throw new RepositoryException("File uses schema version " + version
                    + ", but this build only supports version "
                    + GradeBookDto.CURRENT_SCHEMA_VERSION + ".");
        }

        List<SemesterDto> semesters = dto.semesters();

        if (version < 2) {
            semesters = addPlannedExams(semesters);
            version = 2;
        }

        return new GradeBookDto(GradeBookDto.CURRENT_SCHEMA_VERSION, semesters);
    }

    /**
     * Version 1 → 2: geplante Prüfungen kommen dazu.
     *
     * <p>Eine Datei der Version 1 kennt sie nicht, also beginnt jedes Modul mit
     * einer leeren Liste. Es geht nichts verloren, und der Schritt steht
     * trotzdem ausdrücklich hier – sonst wäre beim nächsten Formatwechsel nicht
     * mehr zu sehen, wann das Feld dazugekommen ist.</p>
     */
    private static List<SemesterDto> addPlannedExams(List<SemesterDto> semesters) {
        return semesters.stream()
                .filter(semester -> semester != null)
                .map(semester -> new SemesterDto(semester.id(), semester.name(),
                        semester.modules().stream()
                                .filter(module -> module != null)
                                .map(module -> new ModuleDto(module.id(), module.name(),
                                        module.grades(), List.of()))
                                .toList()))
                .toList();
    }
}
