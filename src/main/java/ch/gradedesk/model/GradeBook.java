package ch.gradedesk.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Wurzel des Datenmodells: alle Semester einer Person.
 *
 * <p>Die Semesterliste ist so aufgebaut, dass sie über ihre Extraktoren auch
 * Änderungen an Modulen und Noten weitermeldet. Ein einziger Listener auf
 * {@link #getSemesters()} genügt deshalb, um jede Datenänderung mitzubekommen
 * (genutzt vom automatischen Speichern und vom Diagramm).</p>
 */
public final class GradeBook {

    private final ObservableList<Semester> semesters =
            FXCollections.observableArrayList(Semester::observables);

    /** Alle Semester. */
    public ObservableList<Semester> getSemesters() {
        return semesters;
    }

    /** Gesamtzahl erfasster Noten. */
    public long countGrades() {
        return semesters.stream()
                .flatMap(semester -> semester.getModules().stream())
                .mapToLong(module -> module.getGrades().size())
                .sum();
    }

    /** Gesamtzahl Module. */
    public long countModules() {
        return semesters.stream().mapToLong(semester -> semester.getModules().size()).sum();
    }

    /** Anzahl Module mit einer ungenügenden Modulnote. */
    public long countInsufficientModules() {
        return semesters.stream()
                .flatMap(semester -> semester.getModules().stream())
                .map(CourseModule::getRoundedGrade)
                .filter(Objects::nonNull)
                .filter(grade -> grade.compareTo(new BigDecimal("4.0")) < 0)
                .count();
    }

    /** Ersetzt den gesamten Inhalt, z.B. nach dem Laden aus der Datei. */
    public void replaceAll(Iterable<Semester> newSemesters) {
        semesters.clear();
        newSemesters.forEach(semesters::add);
    }
}
