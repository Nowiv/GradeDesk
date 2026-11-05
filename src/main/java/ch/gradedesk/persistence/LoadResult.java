package ch.gradedesk.persistence;

import ch.gradedesk.model.Semester;

import java.util.List;

/**
 * Ergebnis eines Ladevorgangs: die Daten und wie sie zustande kamen.
 *
 * <p>Bewusst ohne fertigen Meldungstext. Der Status sagt, was geschehen ist,
 * {@link #detail()} nennt bei Bedarf den Dateinamen – die Formulierung entsteht
 * im UI in der eingestellten Sprache.</p>
 *
 * @param semesters die geladenen Semester, nie {@code null}
 * @param status    wie der Ladevorgang ausgegangen ist
 * @param detail    ergänzende Angabe, meist ein Dateiname, oder {@code null}
 */
public record LoadResult(List<Semester> semesters, Status status, String detail) {

    public LoadResult {
        semesters = semesters == null ? List.of() : List.copyOf(semesters);
    }

    /** Erfolgreich geladen, ohne Besonderheiten. */
    public static LoadResult loaded(List<Semester> semesters) {
        return new LoadResult(semesters, Status.LOADED, null);
    }

    /** Es gibt noch keine gespeicherten Daten – ein frischer Start. */
    public static LoadResult noData() {
        return new LoadResult(List.of(), Status.NO_DATA, null);
    }

    /** Die Hauptdatei war unlesbar, das Backup konnte einspringen. */
    public static LoadResult recoveredFromBrokenFile(List<Semester> semesters, String quarantinedFile) {
        return new LoadResult(semesters, Status.RECOVERED_FROM_BROKEN_FILE, quarantinedFile);
    }

    /** Die Hauptdatei fehlte, das Backup konnte einspringen. */
    public static LoadResult recoveredFromMissingFile(List<Semester> semesters) {
        return new LoadResult(semesters, Status.RECOVERED_FROM_MISSING_FILE, null);
    }

    /** Weder Hauptdatei noch Backup waren lesbar. */
    public static LoadResult corrupted(String quarantinedFile) {
        return new LoadResult(List.of(), Status.CORRUPTED, quarantinedFile);
    }

    /** {@code true}, wenn der Benutzer über den Ausgang informiert werden sollte. */
    public boolean needsAttention() {
        return status.needsAttention;
    }

    /** Ausgang eines Ladevorgangs. */
    public enum Status {
        /** Daten wurden normal geladen. */
        LOADED(false),
        /** Es waren noch keine Daten vorhanden. */
        NO_DATA(false),
        /** Die Hauptdatei war beschädigt, das Backup sprang ein. */
        RECOVERED_FROM_BROKEN_FILE(true),
        /** Die Hauptdatei fehlte, das Backup sprang ein. */
        RECOVERED_FROM_MISSING_FILE(true),
        /** Hauptdatei und Backup waren unlesbar; die Anwendung startet leer. */
        CORRUPTED(true);

        private final boolean needsAttention;

        Status(boolean needsAttention) {
            this.needsAttention = needsAttention;
        }
    }
}
