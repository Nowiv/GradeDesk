# Notenrechner

Eine lokale Desktop-Anwendung zum Erfassen und Auswerten von Schulnoten nach der
Schweizer Notenskala (1.0 bis 6.0, genügend ab 4.0).

Die Noten werden nach **Semester → Modul → Note** strukturiert. Die Anwendung
rechnet den gewichteten Modulschnitt, rundet ihn kaufmännisch auf eine halbe Note
und bildet daraus den Semesterschnitt. Jede Änderung wird sofort gespeichert.

**Die Anwendung ist rein lokal.** Keine Netzwerkverbindung, kein Konto, kein
Cloud-Abgleich, keine Telemetrie, keine Export- oder Teilen-Funktion.

---

## Inhalt

- [Funktionen](#funktionen)
- [Fachliche Regeln](#fachliche-regeln)
- [Wo die Daten liegen](#wo-die-daten-liegen)
- [Lokal starten](#lokal-starten)
- [Tests](#tests)
- [Installer lokal bauen](#installer-lokal-bauen)
- [Release über GitHub erstellen](#release-über-github-erstellen)
- [Warnungen bei unsignierten Installern](#warnungen-bei-unsignierten-installern)
- [Aufbau des Projekts](#aufbau-des-projekts)
- [Erweitern](#erweitern)

---

## Funktionen

- Semester, Module und Noten anlegen, bearbeiten und löschen (Löschen immer mit
  Bestätigung, die benennt, was verloren geht)
- Übersicht pro Semester mit allen Modulen, Modulnote, ungerundetem Durchschnitt
  und genügend/ungenügend-Status
- Säulendiagramm aller Modulnoten, nach Semester gruppiert, mit fixer Y-Achse
  von 1 bis 6, Referenzlinien bei jeder ganzen Note und hervorgehobener
  Genügend-Grenze bei 4.0
- Automatisches Speichern nach jeder Änderung, atomar und mit Backup
- Vollständige Tastaturbedienung, frei skalierbares Fenster

## Fachliche Regeln

| Regel | Verhalten |
|---|---|
| Notenskala | 1.0 bis 6.0; 1.0 ist die schlechteste, 6.0 die beste Note |
| Genügend | ab 4.0 |
| Ungültige Eingabe | Werte ausserhalb 1.0–6.0 werden mit einer klaren Meldung abgelehnt |
| Gewichtung | Standard 1.0, frei wählbar, muss grösser als 0 sein |
| Modulnote | gewichteter Durchschnitt, kaufmännisch auf 0.5 gerundet |
| Rundungsbeispiele | 4.24 → 4.0, 4.25 → 4.5, 4.74 → 4.5, 4.75 → 5.0 |
| Ungerundeter Wert | wird überall zusätzlich angezeigt |
| Semesterschnitt | Durchschnitt der **gerundeten** Modulnoten |
| Modul ohne Noten | zeigt «–» und fliesst in keine Berechnung ein |

Gerechnet wird durchgehend mit `BigDecimal` und `RoundingMode.HALF_UP`, nie mit
`double`. Damit entstehen keine Gleitkomma-Rundungsfehler.

## Wo die Daten liegen

Die Noten liegen im Benutzerprofil, **nicht** im Installationsordner. Dadurch
bleiben sie bei Updates und Neuinstallationen erhalten, und jeder
Betriebssystem-Benutzer hat automatisch seine eigenen Daten.

| System | Verzeichnis |
|---|---|
| Windows | `%APPDATA%\Notenrechner\` |
| macOS | `~/Library/Application Support/Notenrechner/` |
| Linux | `~/.local/share/notenrechner/` (bzw. `$XDG_DATA_HOME`) |

Darin:

- `noten.json` – der aktuelle Stand, mit Schema-Version für spätere Migrationen
- `noten.backup.json` – der zuletzt gültige Stand
- `noten.beschaedigt-<Zeitstempel>.json` – entsteht nur, wenn eine Datei nicht
  gelesen werden konnte; sie wird beiseitegelegt statt überschrieben

Beim Speichern wird zuerst vollständig in eine temporäre Datei geschrieben, dann
die bisherige Datei zum Backup gemacht und erst danach die neue an ihre Stelle
verschoben. Ein Absturz mitten im Speichern kann deshalb keinen halben Stand
hinterlassen. Ist die Hauptdatei beschädigt, lädt die Anwendung beim Start
automatisch das Backup und sagt es.

Den genauen Pfad zeigt die Anwendung unter **Einstellungen**.

## Lokal starten

Voraussetzung: **JDK 21** oder neuer. Maven wird über den mitgelieferten Wrapper
bezogen, eine Maven-Installation ist nicht nötig.

```bash
# Windows
mvnw.cmd javafx:run

# macOS und Linux
./mvnw javafx:run
```

Zeigt `java -version` eine ältere Version, auf JDK 21 umstellen:

```bash
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"

# macOS und Linux
export JAVA_HOME=/pfad/zum/jdk-21
```

## Tests

```bash
./mvnw test
```

Abgedeckt sind die Rundung inklusive der Grenzfälle 4.24 / 4.25 / 4.74 / 4.75,
die Gewichtung, die Eingabeprüfung, das Speichern und Laden, das Backup und die
Wiederherstellung aus einer beschädigten Datei.

## Installer lokal bauen

`jpackage` erzeugt für das jeweils laufende Betriebssystem einen nativen
Installer, der über `jlink` eine eigene Java-Runtime mitbringt. **Empfänger
brauchen kein Java.** Ein Installer lässt sich immer nur auf der Zielplattform
bauen – ein Windows-Installer also nur unter Windows.

```bash
./mvnw -Pinstaller clean package
```

Das Ergebnis liegt in `target/installer/`:

| System | Datei | Zusätzlich nötig |
|---|---|---|
| Windows | `Notenrechner-1.0.0.msi` (Startmenü- und Desktop-Verknüpfung) | [WiX Toolset 3.x](https://github.com/wixtoolset/wix3/releases) |
| macOS | `Notenrechner-1.0.0.dmg` | – |
| Linux | `notenrechner_1.0.0_amd64.deb` | `fakeroot`, `binutils` |

Das Programmsymbol liegt als Platzhalter in Rot/Weiss unter `packaging/`. Es
lässt sich mit `java packaging/IconGenerator.java` neu erzeugen – wer ein eigenes
Symbol will, ersetzt entweder die Dateien oder passt den Generator an.

## Release über GitHub erstellen

Der Workflow `.github/workflows/release.yml` baut bei jedem Versions-Tag auf
`windows-latest`, `macos-latest` und `ubuntu-latest` und hängt die Installer als
Assets an den GitHub-Release.

```bash
git tag v1.0.0
git push origin main
git push origin v1.0.0
```

Danach entsteht unter *Releases* automatisch ein Eintrag mit `.msi`, `.dmg` und
`.deb`. Der Workflow lässt sich zum Ausprobieren auch von Hand über
*Actions → Release → Run workflow* starten; dann entsteht kein Release, die
Installer liegen aber als Artefakte zum Herunterladen bereit.

Die Versionsnummer steht in `pom.xml` unter `<version>` und muss zum Tag passen.

## Warnungen bei unsignierten Installern

Die Installer sind nicht mit einem Zertifikat signiert. Das ist kein Fehler,
sondern der normale Zustand ohne gekauftes Code-Signing-Zertifikat. Beide
Systeme zeigen deshalb eine Warnung:

- **Windows (SmartScreen):** «Der Computer wurde durch Windows geschützt.»
  → *Weitere Informationen* → *Trotzdem ausführen*.
- **macOS (Gatekeeper):** «Notenrechner kann nicht geöffnet werden, da der
  Entwickler nicht verifiziert werden kann.» → Im Finder mit der rechten
  Maustaste auf die App → *Öffnen* → im Dialog nochmals *Öffnen*. Alternativ
  unter *Systemeinstellungen → Datenschutz & Sicherheit → Dennoch öffnen*.
- **Linux:** keine Warnung.

Wer die Warnung vermeiden will, braucht ein Code-Signing-Zertifikat (Windows)
bzw. eine Apple-Developer-ID samt Notarisierung (macOS).

## Aufbau des Projekts

```
src/main/java/ch/notenrechner/
├── NotenrechnerApp.java        Einstiegspunkt, verdrahtet alle Bausteine
├── model/                      Datenmodell mit JavaFX-Properties
│   ├── GradeBook, Semester, CourseModule, Grade
├── service/                    Fachlogik, ohne jede UI-Abhängigkeit
│   ├── GradeCalculator         Durchschnitt und Rundung (BigDecimal)
│   ├── GradeScale              die Skala 1.0–6.0
│   ├── GradeValidator          Eingabeprüfung mit anzeigbaren Meldungen
│   ├── GradeBookService        Anlegen, Ändern, Löschen
│   └── AutoSaveService         automatisches Speichern
├── persistence/                Ablage hinter einem Interface
│   ├── GradeRepository         das Interface
│   ├── JsonGradeRepository     JSON-Datei, atomar, mit Backup
│   ├── AppPaths                Datenverzeichnis je Betriebssystem
│   ├── GradeBookMapper         Modell ↔ Datei
│   ├── SchemaMigrator          Migration älterer Dateiformate
│   └── dto/                    Serialisierungsform (Records)
└── ui/                         FXML, Controller und Hilfsklassen
    ├── MainController          Seitenleiste und Ansichtswechsel
    ├── Views                   Liste der Ansichten (Erweiterungspunkt)
    ├── view/                   die vier Ansichten
    ├── chart/GradeBarChart     das Säulendiagramm
    ├── control/                Notenband und Dialoge
    └── util/                   Formatierung, Symbole, Theme
```

```
src/main/resources/ch/notenrechner/
├── css/theme.css               alle Farben an einer Stelle
├── css/app.css                 die Gestaltung
├── ui/*.fxml                   Grundgerüst und Ansichten
└── icon/                       Programmsymbol
```

Die Trennung ist strikt: `model` und `service` kennen kein JavaFX-UI,
`persistence` kennt nur das Modell, und im Java- und FXML-Code steht kein
einziges Inline-Styling – die Oberfläche wird ausschliesslich über Stilklassen
und CSS gestaltet.

Die gestalterischen Entscheide stehen in [DESIGN.md](DESIGN.md), die
Produktfestlegungen in [PRODUCT.md](PRODUCT.md).

## Erweitern

### Eine neue Ansicht ergänzen

Die Seitenleiste wird aus einer Liste erzeugt. Eine zusätzliche Ansicht braucht
drei Dinge und keinen Umbau am Bestehenden:

1. **FXML anlegen** unter `src/main/resources/ch/notenrechner/ui/view/`, zum
   Beispiel `statistik-view.fxml`. Als Vorlage dient `overview-view.fxml`: oben
   ein `page-header`, darunter der Inhalt.
2. **Controller schreiben** in `ch.notenrechner.ui.view`, der
   `ViewController` umsetzt:

   ```java
   public final class StatistikController implements ViewController {
       @Override
       public void init(AppServices services) {
           // services.gradeBook() liefert den Service mit dem Modell
       }
   }
   ```

3. **Eintrag ergänzen** in `ch.notenrechner.ui.Views#entries()`:

   ```java
   new NavigationEntry("statistik", "Statistik", Icons.CHART,
           BASE + "statistik-view.fxml", StatistikController::new)
   ```

Die Schaltfläche in der Seitenleiste, das Laden, das Zwischenspeichern der
Ansicht und der Wechsel entstehen daraus von selbst. Wird im Controller ein
neues Symbol gebraucht, kommt der SVG-Pfad in `ui/util/Icons`.

### Die Farben ändern

Alle Farben stehen als Variablen in
`src/main/resources/ch/notenrechner/css/theme.css`. Dort genügt es, die Werte zu
ändern; im übrigen Code steht kein einziger Farbwert.

### Eine andere Speicherart einbauen

`GradeRepository` ist das einzige, was die Anwendung von der Ablage kennt. Für
eine SQLite-Variante genügt eine zweite Implementierung dieses Interface; in
`NotenrechnerApp` wird dann statt `new JsonGradeRepository()` die neue Klasse
eingesetzt. UI und Services bleiben unverändert.

### Das Dateiformat erweitern

`GradeBookDto.CURRENT_SCHEMA_VERSION` erhöhen und in `SchemaMigrator#migrate`
einen Schritt ergänzen, der von der Vorgängerversion übersetzt. Dateien mit einer
höheren Version als die installierte Anwendung werden bewusst nicht geöffnet,
damit eine ältere Programmversion keine Daten verliert.
