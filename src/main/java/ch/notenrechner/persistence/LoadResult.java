package ch.notenrechner.persistence;

import ch.notenrechner.model.Semester;

import java.util.List;

/**
 * Ergebnis eines Ladevorgangs: die Daten und die Information, wie sie zustande kamen.
 * Das UI entscheidet anhand des Status, ob es den Benutzer informieren muss.
 *
 * @param semesters die geladenen Semester, nie {@code null}
 * @param status    wie der Ladevorgang ausgegangen ist
 * @param message   ergänzende Erklärung für den Benutzer, oder {@code null}
 */
public record LoadResult(List<Semester> semesters, Status status, String message) {

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

    /** Aus dem Backup wiederhergestellt, weil die Hauptdatei beschädigt war. */
    public static LoadResult recovered(List<Semester> semesters, String message) {
        return new LoadResult(semesters, Status.RECOVERED_FROM_BACKUP, message);
    }

    /** Weder Hauptdatei noch Backup waren lesbar. */
    public static LoadResult corrupted(String message) {
        return new LoadResult(List.of(), Status.CORRUPTED, message);
    }

    /** {@code true}, wenn der Benutzer über den Ausgang informiert werden sollte. */
    public boolean needsAttention() {
        return status == Status.RECOVERED_FROM_BACKUP || status == Status.CORRUPTED;
    }

    /** Ausgang eines Ladevorgangs. */
    public enum Status {
        /** Daten wurden normal geladen. */
        LOADED,
        /** Es waren noch keine Daten vorhanden. */
        NO_DATA,
        /** Die Hauptdatei war unlesbar, das Backup konnte einspringen. */
        RECOVERED_FROM_BACKUP,
        /** Hauptdatei und Backup waren unlesbar; die Anwendung startet leer. */
        CORRUPTED
    }
}
