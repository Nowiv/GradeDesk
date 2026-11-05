package ch.notenrechner.ui;

import ch.notenrechner.service.AutoSaveService;
import ch.notenrechner.service.GradeBookService;

/**
 * Die Dienste, die jede Ansicht benötigt. Statt eines Frameworks genügt hier
 * eine bewusst einfache Übergabe im Konstruktor der Anwendung.
 */
public record AppServices(GradeBookService gradeBook, AutoSaveService autoSave) {
}
