# Product

<!-- impeccable:product-schema 1 -->

## Platform

desktop

Java-Desktop-Anwendung (JavaFX) für Windows, macOS und Linux. Das Schema kennt
diesen Wert nicht offiziell; `web` wäre falsch, deshalb steht hier `desktop`.

## Stack

Java 21 (LTS), JavaFX 21, Maven mit `javafx-maven-plugin`, modulares Projekt
(`module-info.java`) für jlink/jpackage. JSON-Persistenz mit Jackson, Tests mit
JUnit 5. UI als FXML mit Controllern, Gestaltung ausschliesslich über CSS.
Vom Auftraggeber vorgegeben, nicht delegiert.

## Users

Eine einzelne Person: Lernende bzw. Studierende an einer Berufsschule, HF oder
in der Weiterbildung in der Schweiz. Sie erfasst ihre eigenen Noten auf ihrem
privaten Rechner, meist kurz nachdem eine Prüfung zurückkommt, und schaut
zwischendurch nach, wie sie steht. Keine Lehrpersonen, keine Klassen, keine
Mehrbenutzer-Szenarien innerhalb der Anwendung: Mehrbenutzerfähigkeit entsteht
allein dadurch, dass jeder Betriebssystem-Benutzer sein eigenes Datenverzeichnis
hat.

## Product Purpose

Die Anwendung beantwortet eine Frage verlässlich: «Wie stehe ich gerade?» Sie
erfasst Noten nach Semester und Modul, rechnet Modul- und Semesterschnitte nach
den Regeln der Schweizer Notenskala und zeigt auf einen Blick, welche Module
genügend und welche ungenügend sind. Erfolg heisst: die Person vertraut der
Zahl, die dort steht, und muss nichts nachrechnen.

## Positioning

Rein lokal und privat. Keine Netzwerkverbindung, kein Konto, kein Cloud-Abgleich,
keine Telemetrie, keine Export- oder Teilen-Funktion. Noten sind sensible Daten;
der ganze Nutzen entsteht, ohne dass sie das Gerät je verlassen. Das unterscheidet
die Anwendung von den verbreiteten Web- und Handy-Notenrechnern.

## Operating Context

- Desktop-Fenster, frei skalierbar, Maus und Tastatur.
- Typische Sitzung: kurz. Eine Note erfassen oder den Stand prüfen, dann schliessen.
- Häufigste Aufgabe gemäss Auftraggeber: **Überblick und Stand prüfen**. Die
  Übersicht ist der Startbildschirm.
- Begriffe aus dem Berufsbildungs-Umfeld: Semester, Modul, Note, Gewichtung.
  «Modul» ist die richtige Bezeichnung, nicht «Fach».
- Ein Semester enthält Module, ein Modul enthält Noten. Diese Hierarchie ist fix.

## Capabilities and Constraints

- Notenskala 1.0–6.0. 1.0 ist die schlechteste, 6.0 die beste Note. Ab 4.0
  genügend. Eingaben ausserhalb der Skala werden mit einer klaren Meldung
  abgelehnt.
- Jede Note hat Wert, Gewichtung (Standard 1.0, frei wählbar, grösser als 0),
  Bezeichnung und Datum.
- Modulnote = gewichteter Durchschnitt, kaufmännisch auf 0.5 gerundet
  (4.24 → 4.0, 4.25 → 4.5). Der ungerundete Durchschnitt wird zusätzlich gezeigt.
- Semesterdurchschnitt = Durchschnitt der gerundeten Modulnoten.
- Module ohne Noten zeigen «–» und fliessen nirgends in eine Berechnung ein.
- Alle Berechnungen laufen über `BigDecimal`, nie über `double`.
- Änderungen werden sofort automatisch gespeichert; es gibt keinen Speichern-Knopf.
- Löschen immer mit Bestätigung.
- Daten liegen als JSON im Benutzerverzeichnis des Betriebssystems, nie im
  Installationsordner. Atomares Schreiben, Backup vor jedem Speichern,
  Schema-Version für spätere Migrationen.
- Der Installer enthält keine Daten. Nach der Installation startet die
  Anwendung immer leer.
- Erster Start: geführter Einstieg. Der leere Zustand erklärt in einem Schritt,
  was zu tun ist, und legt auf Klick das erste Semester an. Keine Beispieldaten.
- Nicht vorgesehen: ECTS-gewichtete Semesterschnitte, Zielnoten-Rechner,
  Export, Drucken, Mehrbenutzerverwaltung.

## Brand Commitments

- Name: **Notenrechner**.
- Hauptfarben Rot und Weiss, als Verweis auf die Schweiz. Weiss trägt die
  Fläche, Rot setzt Akzente in Navigation, Kopfbereich und Hauptaktionen.
- Rot und Grün im Diagramm bedeuten ausschliesslich Notenwerte (ungenügend bzw.
  genügend und besser) und sind getrennt von der Markenfarbe zu lesen.
- Alle Farben als Variablen in einer zentralen Theme-Datei, damit sie später
  ohne Suche im Code änderbar sind.
- Sprache: Deutsch (Schweiz). Kein «ß».

## Evidence on Hand

Keine echten Notendaten, keine Nutzerforschung, keine Referenzkunden. Die
Anwendung startet leer und darf keine erfundenen Beispieldaten mitliefern; alle
Screenshots für Dokumentation müssen mit selbst erfassten Daten entstehen.
Als Programmsymbol existiert nur ein Platzhalter in Rot/Weiss.

## Product Principles

1. **Der Zahl muss man trauen können.** Rundung und Gewichtung sind
   nachvollziehbar: neben jeder gerundeten Note steht der ungerundete Wert.
2. **Nichts verlässt das Gerät.** Jede Funktion muss ohne Netzwerk auskommen.
3. **Kein Datenverlust.** Automatisch speichern, atomar schreiben, immer ein
   Backup. Löschen nur nach Bestätigung.
4. **Leer ist ein gültiger Zustand.** Kein Semester, kein Modul, kein Modul
   ohne Noten darf zu einer falschen Zahl oder einem Fehler führen.
5. **Kurze Sitzungen.** Der Stand muss in der ersten Sekunde lesbar sein, ohne
   dass jemand etwas aufklappt.

## Accessibility & Inclusion

Keine vom Auftraggeber genannte Norm. Eigener Anspruch: Status darf nie allein
über Farbe transportiert werden (Rot/Grün-Sehschwäche ist bei einer
Notenanwendung ein realistischer Fall), Kontraste nach WCAG AA, vollständige
Tastaturbedienung, Fenster frei skalierbar ohne abgeschnittene Inhalte.
