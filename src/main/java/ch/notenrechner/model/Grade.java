package ch.notenrechner.model;

import ch.notenrechner.service.GradeCalculator;
import ch.notenrechner.service.GradeValidator;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Eine einzelne Note innerhalb eines Moduls.
 *
 * <p>Alle Felder sind JavaFX-Properties, damit das UI und das Diagramm ohne
 * zusätzlichen Benachrichtigungscode live auf Änderungen reagieren.</p>
 */
public final class Grade implements GradeCalculator.WeightedValue {

    /** Standardgewichtung einer Note. */
    public static final BigDecimal DEFAULT_WEIGHT = BigDecimal.ONE;

    private final String id;
    private final StringProperty label = new SimpleStringProperty(this, "label", "");
    private final ObjectProperty<BigDecimal> value = new SimpleObjectProperty<>(this, "value");
    private final ObjectProperty<BigDecimal> weight =
            new SimpleObjectProperty<>(this, "weight", DEFAULT_WEIGHT);
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(this, "date");

    /** Erzeugt eine neue Note mit frischer Identität. */
    public Grade(String label, BigDecimal value, BigDecimal weight, LocalDate date) {
        this(UUID.randomUUID().toString(), label, value, weight, date);
    }

    /** Erzeugt eine Note mit bestehender Identität (z.B. beim Laden aus der Datei). */
    public Grade(String id, String label, BigDecimal value, BigDecimal weight, LocalDate date) {
        this.id = Objects.requireNonNull(id, "id");
        setLabel(label);
        setValue(value);
        setWeight(weight);
        setDate(date);
    }

    /**
     * Setzt alle fachlichen Felder in einem Schritt, nachdem sie validiert wurden.
     * So kann eine ungültige Eingabe die Note nicht halb verändert zurücklassen.
     */
    public void update(String newLabel, BigDecimal newValue, BigDecimal newWeight, LocalDate newDate) {
        String validLabel = GradeValidator.requireValidText(newLabel, "eine Bezeichnung");
        BigDecimal validValue = GradeValidator.requireValidGrade(newValue);
        BigDecimal validWeight = GradeValidator.requireValidWeight(newWeight);
        LocalDate validDate = GradeValidator.requireValidDate(newDate);

        setLabel(validLabel);
        setValue(validValue);
        setWeight(validWeight);
        setDate(validDate);
    }

    /** Stabile Identität, bleibt über das Speichern und Laden hinweg erhalten. */
    public String getId() {
        return id;
    }

    public StringProperty labelProperty() {
        return label;
    }

    public String getLabel() {
        return label.get();
    }

    public void setLabel(String label) {
        this.label.set(label == null ? "" : label);
    }

    public ObjectProperty<BigDecimal> valueProperty() {
        return value;
    }

    public BigDecimal getValue() {
        return value.get();
    }

    public void setValue(BigDecimal value) {
        this.value.set(value);
    }

    public ObjectProperty<BigDecimal> weightProperty() {
        return weight;
    }

    public BigDecimal getWeight() {
        return weight.get();
    }

    public void setWeight(BigDecimal weight) {
        this.weight.set(weight == null ? DEFAULT_WEIGHT : weight);
    }

    public ObjectProperty<LocalDate> dateProperty() {
        return date;
    }

    public LocalDate getDate() {
        return date.get();
    }

    public void setDate(LocalDate date) {
        this.date.set(date);
    }

    @Override
    public BigDecimal value() {
        return getValue();
    }

    @Override
    public BigDecimal weight() {
        return getWeight();
    }

    /**
     * Die Properties, auf die eine beobachtbare Liste von Noten reagieren soll.
     * Dadurch melden sich Änderungen an einer Note bei der umgebenden Liste.
     */
    public static Observable[] observables(Grade grade) {
        return new Observable[] {
                grade.labelProperty(), grade.valueProperty(),
                grade.weightProperty(), grade.dateProperty()
        };
    }

    @Override
    public String toString() {
        return getLabel() + " " + (getValue() == null ? "-" : getValue().toPlainString());
    }
}
