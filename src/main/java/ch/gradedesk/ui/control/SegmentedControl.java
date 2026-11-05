package ch.gradedesk.ui.control;

import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Ein segmentiertes Bedienelement nach dem Vorbild von iOS.
 *
 * <p>Alle Möglichkeiten liegen nebeneinander sichtbar auf einer vertieften
 * Spur; die aktive sitzt als heller Reiter darauf. Gegenüber einem Auswahlfeld
 * hat das zwei Vorteile: man sieht ohne Öffnen, was es gibt, und der Wechsel
 * kostet einen Klick. Apple empfiehlt das bis etwa fünf Möglichkeiten.</p>
 *
 * <p>Die aktive Wahl lässt sich nicht abwählen – «nichts gewählt» wäre bei
 * Sprache oder Erscheinungsbild kein sinnvoller Zustand.</p>
 *
 * @param <T> der Typ der Auswahlmöglichkeiten, meist ein Aufzählungstyp
 */
public class SegmentedControl<T> extends HBox {

    private final ToggleGroup group = new ToggleGroup();
    private boolean updating;

    /**
     * @param options   alle Möglichkeiten in der Reihenfolge der Anzeige
     * @param labelOf   liefert die Beschriftung zu einer Möglichkeit
     * @param selected  die anfangs aktive Möglichkeit
     * @param onSelect  wird bei jeder Änderung durch den Benutzer aufgerufen
     */
    public SegmentedControl(List<T> options, Function<T, String> labelOf, T selected,
                            Consumer<T> onSelect) {
        getStyleClass().add("segmented");
        setFocusTraversable(false);

        for (T option : options) {
            ToggleButton segment = new ToggleButton(labelOf.apply(option));
            segment.getStyleClass().add("segmented__option");
            segment.setUserData(option);
            segment.setToggleGroup(group);
            segment.setMaxWidth(Double.MAX_VALUE);
            segment.setAccessibleText(labelOf.apply(option));
            setHgrow(segment, Priority.ALWAYS);
            getChildren().add(segment);
        }

        select(selected);

        group.selectedToggleProperty().addListener((observable, previous, current) -> {
            if (current == null) {
                group.selectToggle(previous);
                return;
            }
            if (!updating) {
                @SuppressWarnings("unchecked")
                T value = (T) current.getUserData();
                onSelect.accept(value);
            }
        });
    }

    /** Setzt die Anzeige auf diesen Wert, ohne {@code onSelect} auszulösen. */
    public void select(T value) {
        updating = true;
        try {
            for (Toggle toggle : group.getToggles()) {
                if (toggle.getUserData() == null ? value == null : toggle.getUserData().equals(value)) {
                    group.selectToggle(toggle);
                    return;
                }
            }
        } finally {
            updating = false;
        }
    }
}
