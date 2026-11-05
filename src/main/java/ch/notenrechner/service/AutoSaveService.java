package ch.notenrechner.service;

import ch.notenrechner.model.GradeBook;
import ch.notenrechner.persistence.RepositoryException;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Speichert jede Änderung automatisch.
 *
 * <p>Ein einzelner Listener auf der Semesterliste genügt: das Modell meldet
 * Änderungen an Modulen und Noten über seine Extraktoren nach oben durch.</p>
 *
 * <p>Mehrere Änderungen in schneller Folge – etwa beim Tippen in einer Tabelle –
 * werden zu einem Schreibvorgang zusammengefasst. Das Schreiben selbst läuft
 * anschliessend wieder auf dem UI-Thread, damit der Datenstand nicht gelesen
 * wird, während er gerade verändert wird.</p>
 */
public final class AutoSaveService implements AutoCloseable {

    /** Wartezeit, in der weitere Änderungen zum selben Schreibvorgang zusammengefasst werden. */
    public static final long DEFAULT_DELAY_MILLIS = 250;

    private final GradeBookService service;
    private final Executor uiExecutor;
    private final long delayMillis;
    private final ScheduledExecutorService scheduler;
    private final ObjectProperty<SaveState> state =
            new SimpleObjectProperty<>(this, "state", SaveState.IDLE);

    private final StringProperty lastError = new SimpleStringProperty(this, "lastError", "");

    private ScheduledFuture<?> pending;
    private boolean active;

    /**
     * @param service    der Service, dessen Modell beobachtet und gespeichert wird
     * @param uiExecutor führt das Speichern auf dem UI-Thread aus (in Tests: direkt)
     */
    public AutoSaveService(GradeBookService service, Executor uiExecutor) {
        this(service, uiExecutor, DEFAULT_DELAY_MILLIS);
    }

    public AutoSaveService(GradeBookService service, Executor uiExecutor, long delayMillis) {
        this.service = Objects.requireNonNull(service, "service");
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
        this.delayMillis = delayMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "notenrechner-autosave");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Beginnt, Änderungen zu beobachten. */
    public void start() {
        if (active) {
            return;
        }
        active = true;
        GradeBook gradeBook = service.getGradeBook();
        gradeBook.getSemesters().addListener((InvalidationListener) observable -> requestSave());
    }

    /** Der aktuelle Speicherzustand, für die Statusanzeige im UI. */
    public ReadOnlyObjectProperty<SaveState> stateProperty() {
        return state;
    }

    public SaveState getState() {
        return state.get();
    }

    /** Die Meldung des letzten fehlgeschlagenen Speichervorgangs, sonst leer. */
    public ReadOnlyStringProperty lastErrorProperty() {
        return lastError;
    }

    private void requestSave() {
        state.set(SaveState.PENDING);
        if (pending != null) {
            pending.cancel(false);
        }
        if (delayMillis <= 0) {
            uiExecutor.execute(this::writeNow);
            return;
        }
        pending = scheduler.schedule(
                () -> uiExecutor.execute(this::writeNow), delayMillis, TimeUnit.MILLISECONDS);
    }

    /** Schreibt einen noch ausstehenden Stand sofort, z.B. beim Beenden der Anwendung. */
    public void flush() {
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
        if (state.get() == SaveState.PENDING || state.get() == SaveState.FAILED) {
            writeNow();
        }
    }

    private void writeNow() {
        try {
            service.save();
            lastError.set("");
            state.set(SaveState.SAVED);
        } catch (RepositoryException e) {
            // Nicht weiterwerfen: ein fehlgeschlagenes Speichern darf die laufende
            // Bearbeitung nicht abbrechen. Das UI zeigt den Zustand an.
            lastError.set(e.getMessage() == null ? "Unbekannter Fehler." : e.getMessage());
            state.set(SaveState.FAILED);
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }

    /** Zustand des automatischen Speicherns. */
    public enum SaveState {
        /** Seit dem Start wurde nichts geändert. */
        IDLE,
        /** Eine Änderung wartet darauf, geschrieben zu werden. */
        PENDING,
        /** Der aktuelle Stand ist geschrieben. */
        SAVED,
        /** Das Schreiben ist fehlgeschlagen. */
        FAILED
    }
}
