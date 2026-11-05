package ch.gradedesk.ui;

/**
 * Gemeinsamer Vertrag aller Ansichten.
 *
 * <p>Eine neue Ansicht besteht aus einer FXML-Datei, einem Controller, der dieses
 * Interface umsetzt, und einem Eintrag in {@link Views#entries()}. Am bestehenden
 * Aufbau ist dafür nichts zu ändern.</p>
 */
public interface ViewController {

    /** Wird einmal nach dem Laden der FXML-Datei aufgerufen. */
    void init(AppServices services);

    /** Wird jedes Mal aufgerufen, wenn die Ansicht sichtbar wird. */
    default void onShown() {
    }
}
