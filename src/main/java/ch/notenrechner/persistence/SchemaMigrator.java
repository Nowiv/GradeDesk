package ch.notenrechner.persistence;

import ch.notenrechner.persistence.dto.GradeBookDto;

/**
 * Bringt eine geladene Datei auf das aktuelle Dateiformat.
 *
 * <p>Erweiterungspunkt für künftige Formatänderungen: pro neuer Version kommt ein
 * weiterer Schritt hinzu, der die Daten von der Vorgängerversion übersetzt. Dateien
 * einer neueren Version als der Anwendung werden bewusst nicht geöffnet, damit eine
 * ältere Programmversion keine Daten verliert.</p>
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
            throw new RepositoryException("Die Datei wurde mit einer neueren Version von "
                    + "Notenrechner erstellt (Format " + version + ", unterstützt wird "
                    + GradeBookDto.CURRENT_SCHEMA_VERSION + ").");
        }

        // Version 1 ist das aktuelle Format: hier sind noch keine Schritte nötig.
        return new GradeBookDto(GradeBookDto.CURRENT_SCHEMA_VERSION, dto.semesters());
    }
}
