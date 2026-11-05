package ch.gradedesk.model;

import ch.gradedesk.service.GradeCalculator;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Ein Modul (Fach) mit beliebig vielen Noten.
 *
 * <p>Der ungerundete Durchschnitt und die gerundete Modulnote sind abgeleitete
 * Properties: sie rechnen sich selbst neu, sobald eine Note hinzukommt, wegfällt
 * oder sich ändert. Ein Modul ohne Noten liefert {@code null} und fliesst damit
 * nirgends in eine Berechnung ein.</p>
 */
public final class CourseModule {

    private final String id;
    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final ObservableList<Grade> grades =
            FXCollections.observableArrayList(Grade::observables);

    private final ObjectProperty<BigDecimal> exactAverage =
            new SimpleObjectProperty<>(this, "exactAverage");
    private final ObjectProperty<BigDecimal> roundedGrade =
            new SimpleObjectProperty<>(this, "roundedGrade");

    public CourseModule(String name) {
        this(UUID.randomUUID().toString(), name);
    }

    public CourseModule(String id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        setName(name);
        exactAverage.bind(Bindings.createObjectBinding(
                () -> GradeCalculator.weightedAverage(grades).orElse(null), grades));
        roundedGrade.bind(Bindings.createObjectBinding(
                () -> GradeCalculator.moduleGrade(grades).orElse(null), grades));
    }

    public String getId() {
        return id;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name == null ? "" : name);
    }

    /** Die Noten dieses Moduls. Änderungen lösen die Neuberechnung aus. */
    public ObservableList<Grade> getGrades() {
        return grades;
    }

    /** Der ungerundete gewichtete Durchschnitt oder {@code null} ohne Noten. */
    public ReadOnlyObjectProperty<BigDecimal> exactAverageProperty() {
        return exactAverage;
    }

    public BigDecimal getExactAverage() {
        return exactAverage.get();
    }

    /** Die auf 0.5 gerundete Modulnote oder {@code null} ohne Noten. */
    public ReadOnlyObjectProperty<BigDecimal> roundedGradeProperty() {
        return roundedGrade;
    }

    public BigDecimal getRoundedGrade() {
        return roundedGrade.get();
    }

    /** {@code true}, wenn das Modul mindestens eine Note hat. */
    public boolean hasGrade() {
        return getRoundedGrade() != null;
    }

    /**
     * Die Properties, auf die eine beobachtbare Liste von Modulen reagieren soll.
     * Die Notenliste ist mit aufgeführt, damit sich Änderungen tief im Baum
     * bis zur Modulliste durchmelden.
     */
    public static Observable[] observables(CourseModule module) {
        return new Observable[] { module.nameProperty(), module.getGrades() };
    }

    @Override
    public String toString() {
        return getName();
    }
}
