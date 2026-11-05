package ch.notenrechner.ui.view;

import ch.notenrechner.model.GradeBook;
import ch.notenrechner.persistence.JsonGradeRepository;
import ch.notenrechner.persistence.dto.GradeBookDto;
import ch.notenrechner.service.GradeBookService;
import ch.notenrechner.ui.AppServices;
import ch.notenrechner.ui.ViewController;
import ch.notenrechner.ui.util.Icons;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Einstellungen und Angaben zur Anwendung.
 *
 * <p>Es gibt hier bewusst wenig zu schalten. Die Ansicht beantwortet vor allem
 * zwei Fragen: Wo liegen meine Daten, und wer kommt sonst noch daran?</p>
 */
public final class SettingsController implements ViewController {

    private static final String APP_VERSION = "1.0.0";

    private static final List<String> PRIVACY_FACTS = List.of(
            "Die Anwendung baut keine Netzwerkverbindung auf. Es gibt keinen Cloud-Abgleich, "
                    + "kein Konto und keine Telemetrie.",
            "Die Noten liegen ausschliesslich im Profil des angemeldeten Betriebssystem-Benutzers. "
                    + "Andere Benutzer desselben Geräts sehen ihre eigenen, getrennten Daten.",
            "Es gibt keine Export- oder Teilen-Funktion. Was das Gerät verlässt, verlässt es "
                    + "nur, wenn jemand die Datei selbst kopiert.",
            "Der Installer enthält keine Notendaten. Nach einer Neuinstallation startet die "
                    + "Anwendung leer, bestehende Daten im Benutzerprofil bleiben erhalten.");

    @FXML
    private Label pathLabel;
    @FXML
    private Label storageDetailLabel;
    @FXML
    private Label privacyMetaLabel;
    @FXML
    private VBox privacyBox;
    @FXML
    private VBox factsBox;
    @FXML
    private Button copyPathButton;

    private GradeBookService service;
    private GradeBook gradeBook;

    @Override
    public void init(AppServices services) {
        this.service = services.gradeBook();
        this.gradeBook = services.gradeBook().getGradeBook();

        pathLabel.setText(service.describeLocation());
        storageDetailLabel.setText("Daneben liegt «" + JsonGradeRepository.BACKUP_FILE_NAME
                + "»: die Sicherung des letzten gültigen Stands. Geschrieben wird immer zuerst in "
                + "eine temporäre Datei, damit ein Absturz keine halbe Datei hinterlässt.");

        copyPathButton.setGraphic(Icons.create(Icons.COPY, 14));
        copyPathButton.setOnAction(event -> copyPath());

        privacyMetaLabel.setText("Diese Anwendung ist rein lokal.");
        privacyBox.getChildren().add(privacyLead());
        PRIVACY_FACTS.forEach(fact -> privacyBox.getChildren().add(bullet(fact)));

        gradeBook.getSemesters().addListener((InvalidationListener) observable -> refreshFacts());
        refreshFacts();
    }

    @Override
    public void onShown() {
        refreshFacts();
    }

    private void copyPath() {
        ClipboardContent content = new ClipboardContent();
        content.putString(service.describeLocation());
        Clipboard.getSystemClipboard().setContent(content);
        copyPathButton.setText("Pfad kopiert");
        copyPathButton.setDisable(true);
        javafx.animation.PauseTransition reset =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        reset.setOnFinished(event -> {
            copyPathButton.setText("Pfad kopieren");
            copyPathButton.setDisable(false);
        });
        reset.play();
    }

    private void refreshFacts() {
        factsBox.getChildren().setAll(
                fact("Semester", String.valueOf(gradeBook.getSemesters().size())),
                fact("Module", String.valueOf(gradeBook.countModules())),
                fact("Erfasste Noten", String.valueOf(gradeBook.countGrades())),
                fact("Dateiformat", "Version " + GradeBookDto.CURRENT_SCHEMA_VERSION),
                fact("Programmversion", APP_VERSION));
    }

    private Node privacyLead() {
        HBox lead = new HBox(Icons.create(Icons.LOCK, 18));
        Label text = new Label("Noten sind heikle Daten. Die Anwendung ist so gebaut, dass sie "
                + "das Gerät gar nicht erst verlassen können.");
        text.getStyleClass().add("label-body");
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);
        lead.getChildren().add(text);
        lead.setSpacing(10);
        lead.setAlignment(Pos.TOP_LEFT);
        return lead;
    }

    private Node bullet(String text) {
        Label marker = new Label("•");
        marker.getStyleClass().add("label-muted");
        marker.setMinWidth(14);

        Label label = new Label(text);
        label.getStyleClass().add("label-muted");
        label.setWrapText(true);
        HBox.setHgrow(label, Priority.ALWAYS);

        HBox row = new HBox(marker, label);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private Node fact(String name, String value) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("label-caption");
        nameLabel.setMinWidth(170);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("label-body");

        HBox row = new HBox(nameLabel, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
