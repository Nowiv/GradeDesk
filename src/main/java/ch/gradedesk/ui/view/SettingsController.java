package ch.gradedesk.ui.view;

import ch.gradedesk.i18n.I18n;
import ch.gradedesk.model.GradeBook;
import ch.gradedesk.persistence.JsonGradeRepository;
import ch.gradedesk.persistence.dto.GradeBookDto;
import ch.gradedesk.service.GradeBookService;
import ch.gradedesk.ui.AppServices;
import ch.gradedesk.ui.ViewController;
import ch.gradedesk.ui.control.AppearanceSwitch;
import ch.gradedesk.ui.control.LanguageSwitch;
import ch.gradedesk.ui.util.Icons;
import javafx.animation.PauseTransition;
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
import javafx.util.Duration;

import java.util.List;

/**
 * Einstellungen und Angaben zur Anwendung.
 *
 * <p>Es gibt hier bewusst wenig zu schalten. Die Ansicht beantwortet drei
 * Fragen: In welcher Sprache läuft das Programm, wo liegen meine Daten, und
 * wer kommt sonst noch daran?</p>
 */
public final class SettingsController implements ViewController {

    private static final String APP_VERSION = "1.0.0";

    /** Die Datenschutzpunkte, in der Reihenfolge ihrer Schlüssel im Textbündel. */
    private static final List<String> PRIVACY_KEYS = List.of(
            "settings.privacy.1", "settings.privacy.2",
            "settings.privacy.3", "settings.privacy.4");

    @FXML
    private VBox languageSwitchBox;

    @FXML
    private VBox appearanceSwitchBox;
    @FXML
    private Label pathLabel;
    @FXML
    private Label storageDetailLabel;
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

        languageSwitchBox.getChildren().add(new LanguageSwitch());
        appearanceSwitchBox.getChildren().add(new AppearanceSwitch());

        pathLabel.setText(service.describeLocation());
        storageDetailLabel.setText(
                I18n.t("settings.storage.detail", JsonGradeRepository.BACKUP_FILE_NAME));

        copyPathButton.setGraphic(Icons.create(Icons.COPY, 14));
        copyPathButton.setOnAction(event -> copyPath());

        privacyBox.getChildren().add(privacyLead());
        PRIVACY_KEYS.forEach(key -> privacyBox.getChildren().add(bullet(I18n.t(key))));

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

        copyPathButton.setText(I18n.t("settings.pathCopied"));
        copyPathButton.setDisable(true);
        PauseTransition reset = new PauseTransition(Duration.seconds(2));
        reset.setOnFinished(event -> {
            copyPathButton.setText(I18n.t("settings.copyPath"));
            copyPathButton.setDisable(false);
        });
        reset.play();
    }

    private void refreshFacts() {
        factsBox.getChildren().setAll(
                fact(I18n.t("settings.facts.semesters"),
                        String.valueOf(gradeBook.getSemesters().size())),
                fact(I18n.t("settings.facts.modules"),
                        String.valueOf(gradeBook.countModules())),
                fact(I18n.t("settings.facts.grades"),
                        String.valueOf(gradeBook.countGrades())),
                fact(I18n.t("settings.facts.format"),
                        I18n.t("settings.facts.formatValue", GradeBookDto.CURRENT_SCHEMA_VERSION)),
                fact(I18n.t("settings.facts.version"), APP_VERSION));
    }

    private Node privacyLead() {
        Label text = new Label(I18n.t("settings.privacy.lead"));
        text.getStyleClass().add("label-body");
        text.setWrapText(true);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox lead = new HBox(Icons.create(Icons.LOCK, 18), text);
        lead.setSpacing(10);
        lead.setAlignment(Pos.TOP_LEFT);
        return lead;
    }

    private Node bullet(String text) {
        Label marker = new Label("•");
        marker.getStyleClass().add("label-faint");
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
        nameLabel.setMinWidth(180);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("label-body");

        HBox row = new HBox(nameLabel, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
