package ch.gradedesk.ui;

import ch.gradedesk.service.AutoSaveService;
import ch.gradedesk.service.GradeBookService;

/**
 * Die Dienste, die jede Ansicht benötigt. Statt eines Frameworks genügt hier
 * eine bewusst einfache Übergabe im Konstruktor der Anwendung.
 */
public record AppServices(GradeBookService gradeBook, AutoSaveService autoSave) {
}
