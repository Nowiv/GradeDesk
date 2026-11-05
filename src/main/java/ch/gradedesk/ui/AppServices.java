package ch.gradedesk.ui;

import ch.gradedesk.persistence.SettingsStore;
import ch.gradedesk.service.AutoSaveService;
import ch.gradedesk.service.GradeBookService;
import ch.gradedesk.update.UpdateService;
import ch.gradedesk.update.UpdateUi;

/** Dienste, die von den Ansichten und der Hauptnavigation benötigt werden. */
public record AppServices(GradeBookService gradeBook, AutoSaveService autoSave,
                          UpdateService updates, UpdateUi updateUi, SettingsStore settings) {
}
