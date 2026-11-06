package ch.gradedesk.model;

import ch.gradedesk.service.GradeValidator;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/**
 * Eine angekündigte, noch nicht geschriebene Prüfung in einem Modul.
 *
 * <p>Eine geplante Prüfung ist ausdrücklich <em>keine</em> Note: sie hat keinen
 * Wert und fliesst in keine Berechnung ein. Sie hält nur fest, was ansteht –
 * wann, wie es heisst und wie stark es später zählen wird.</p>
 *
 * <p>Die Bezeichnung ist freiwillig. Wer am Montag erfährt, dass in zwei Wochen
 * eine Prüfung stattfindet, weiss das Datum oft eher als den Titel.</p>
 *
 * <p>Sobald die Prüfung zurückkommt, wird daraus eine {@link Grade} – siehe
 * {@code GradeBookService.convertToGrade}. Die geplante Prüfung verschwindet
 * dabei, damit nichts doppelt dasteht.</p>
 */
public final class PlannedExam {

    /** Standardgewichtung, wie bei einer Note. */
    public static final BigDecimal DEFAULT_WEIGHT = BigDecimal.ONE;

    /**
     * Reihenfolge der geplanten Prüfungen: das frühere Datum zuerst.
     *
     * <p>Damit steht eine überfällige Prüfung vor einer künftigen. Das ist
     * gewollt: eine Prüfung, deren Note noch fehlt, ist das Dringendere.</p>
     */
    public static final Comparator<PlannedExam> BY_DATE =
            Comparator.comparing(PlannedExam::getDate);

    private final String id;
    private final StringProperty label = new SimpleStringProperty(this, "label", "");
    private final ObjectProperty<BigDecimal> weight =
            new SimpleObjectProperty<>(this, "weight", DEFAULT_WEIGHT);
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(this, "date");

    /** Erzeugt eine geplante Prüfung mit frischer Identität. */
    public PlannedExam(String label, BigDecimal weight, LocalDate date) {
        this(UUID.randomUUID().toString(), label, weight, date);
    }

    /** Erzeugt eine geplante Prüfung mit bestehender Identität (z.B. beim Laden). */
    public PlannedExam(String id, String label, BigDecimal weight, LocalDate date) {
        this.id = Objects.requireNonNull(id, "id");
        setLabel(label);
        setWeight(weight);
        setDate(date);
    }

    /**
     * Setzt alle fachlichen Felder in einem Schritt, nachdem sie geprüft wurden.
     * Eine ungültige Eingabe kann den Eintrag dadurch nicht halb verändert
     * zurücklassen.
     */
    public void update(String newLabel, BigDecimal newWeight, LocalDate newDate) {
        String validLabel = GradeValidator.requireValidOptionalText(newLabel);
        BigDecimal validWeight = GradeValidator.requireValidWeight(newWeight);
        LocalDate validDate = GradeValidator.requireValidDate(newDate);

        setLabel(validLabel);
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

    /** Die Bezeichnung, möglicherweise leer. */
    public String getLabel() {
        return label.get();
    }

    public void setLabel(String label) {
        this.label.set(label == null ? "" : label);
    }

    /** {@code true}, wenn eine Bezeichnung erfasst ist. */
    public boolean hasLabel() {
        return !getLabel().isBlank();
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

    /**
     * {@code true}, wenn der Prüfungstermin vorbei ist und noch keine Note
     * daraus wurde. Für die Anzeige «Note ausstehend».
     *
     * @param today der heutige Tag, von aussen gegeben, damit das prüfbar bleibt
     */
    public boolean isOverdue(LocalDate today) {
        return getDate() != null && getDate().isBefore(today);
    }

    /**
     * Die Properties, auf die eine beobachtbare Liste geplanter Prüfungen
     * reagieren soll. Dadurch melden sich Änderungen bis zur Modulliste und
     * damit auch beim automatischen Speichern.
     */
    public static Observable[] observables(PlannedExam exam) {
        return new Observable[] {
                exam.labelProperty(), exam.weightProperty(), exam.dateProperty()
        };
    }

    @Override
    public String toString() {
        return (hasLabel() ? getLabel() + " " : "") + getDate();
    }
}
