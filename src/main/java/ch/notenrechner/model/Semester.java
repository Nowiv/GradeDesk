package ch.notenrechner.model;

import ch.notenrechner.service.GradeCalculator;
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Ein Semester mit seinen Modulen.
 *
 * <p>Der Semesterdurchschnitt bildet sich aus den <em>gerundeten</em> Modulnoten.
 * Module ohne Noten werden dabei übersprungen.</p>
 */
public final class Semester {

    private final String id;
    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final ObservableList<CourseModule> modules =
            FXCollections.observableArrayList(CourseModule::observables);

    private final ObjectProperty<BigDecimal> average = new SimpleObjectProperty<>(this, "average");

    public Semester(String name) {
        this(UUID.randomUUID().toString(), name);
    }

    public Semester(String id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        setName(name);
        average.bind(Bindings.createObjectBinding(this::computeAverage, modules));
    }

    private BigDecimal computeAverage() {
        List<BigDecimal> gradedModules = modules.stream()
                .map(CourseModule::getRoundedGrade)
                .filter(Objects::nonNull)
                .toList();
        return GradeCalculator.averageOfModuleGrades(gradedModules).orElse(null);
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

    /** Die Module dieses Semesters in der vom Benutzer gewählten Reihenfolge. */
    public ObservableList<CourseModule> getModules() {
        return modules;
    }

    /** Der Durchschnitt der gerundeten Modulnoten oder {@code null}, wenn kein Modul eine Note hat. */
    public ReadOnlyObjectProperty<BigDecimal> averageProperty() {
        return average;
    }

    public BigDecimal getAverage() {
        return average.get();
    }

    /** Anzahl Module mit mindestens einer Note. */
    public long countGradedModules() {
        return modules.stream().filter(CourseModule::hasGrade).count();
    }

    /** Die Properties, auf die eine beobachtbare Liste von Semestern reagieren soll. */
    public static Observable[] observables(Semester semester) {
        return new Observable[] { semester.nameProperty(), semester.getModules() };
    }

    @Override
    public String toString() {
        return getName();
    }
}
