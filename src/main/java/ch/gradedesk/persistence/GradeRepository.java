package ch.gradedesk.persistence;

import ch.gradedesk.model.Semester;

import java.util.List;

/**
 * Speicherort für die Noten. Die Anwendung kennt nur dieses Interface, nie die
 * konkrete Ablage. So lässt sich die JSON-Datei später z.B. gegen eine
 * SQLite-Datenbank tauschen, ohne UI oder Services anzupassen.
 */
public interface GradeRepository {

    /**
     * Lädt alle Semester. Die Implementierung wirft nicht, wenn noch keine Daten
     * existieren oder die Daten beschädigt sind – das Ergebnis beschreibt den
     * Zustand stattdessen über {@link LoadResult#status()}.
     */
    LoadResult load();

    /**
     * Speichert den vollständigen Datenstand.
     *
     * @param semesters alle Semester in der aktuellen Reihenfolge
     * @throws RepositoryException wenn der Datenstand nicht geschrieben werden konnte
     */
    void save(List<Semester> semesters);

    /**
     * Für den Benutzer lesbare Beschreibung des Speicherorts, z.B. ein Dateipfad.
     * Wird in den Einstellungen angezeigt.
     */
    String describeLocation();
}
