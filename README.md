# Grade Calculator

A local desktop application for recording and analyzing school grades using the Swiss grading scale (1.0 to 6.0, with 4.0 as the passing grade).

Grades are organized by **Semester → Module → Grade**. The application calculates the weighted module average, rounds it commercially to the nearest half grade, and calculates the semester average. Every change is saved automatically.

**The application runs entirely locally.** No network connection, account, cloud synchronization, telemetry, export, or sharing functionality.

---

## Table of Contents

- [Features](#features)
- [Grading Rules](#grading-rules)
- [Where Your Data Is Stored](#where-your-data-is-stored)
- [Running Locally](#running-locally)
- [Tests](#tests)
- [Building Installers Locally](#building-installers-locally)
- [Creating a Release on GitHub](#creating-a-release-on-github)
- [Warnings for Unsigned Installers](#warnings-for-unsigned-installers)
- [Project Structure](#project-structure)
- [Extending the Application](#extending-the-application)

---

## Features

- Create, edit, and delete semesters, modules, and grades (deletions always require confirmation explaining what data will be lost)
- Overview of each semester, including all modules, module grades, unrounded averages, and pass/fail status
- Bar chart displaying all module grades, grouped by semester, with a fixed Y-axis ranging from 1 to 6, reference lines at every whole grade, and a highlighted passing threshold at 4.0
- Automatic saving after every change, using atomic writes and backups
- Full keyboard navigation and a freely resizable window

## Grading Rules

| Rule | Behavior |
|---|---|
| Grading scale | 1.0 to 6.0; 1.0 is the lowest and 6.0 is the highest grade |
| Passing grade | 4.0 or higher |
| Invalid input | Values outside 1.0–6.0 are rejected with a clear error message |
| Weighting | Default: 1.0; freely configurable, must be greater than 0 |
| Module grade | Weighted average, commercially rounded to the nearest 0.5 |
| Rounding examples | 4.24 → 4.0, 4.25 → 4.5, 4.74 → 4.5, 4.75 → 5.0 |
| Unrounded value | Also displayed everywhere |
| Semester average | Average of the **rounded** module grades |
| Module without grades | Displays "–" and is excluded from all calculations |

All calculations consistently use `BigDecimal` and `RoundingMode.HALF_UP`, never `double`. This prevents floating-point rounding errors.

## Where Your Data Is Stored

Grades are stored in the user's profile, **not** in the installation directory. This ensures that data is preserved across updates and reinstalls, and that each operating system user automatically has their own data.

| Operating System | Directory |
|---|---|
| Windows | `%APPDATA%\Notenrechner\` |
| macOS | `~/Library/Application Support/Notenrechner/` |
| Linux | `~/.local/share/notenrechner/` (or `$XDG_DATA_HOME`) |

The directory contains:

- `noten.json` – the current data, including a schema version for future migrations
- `noten.backup.json` – the last valid version of the data
- `noten.beschaedigt-<timestamp>.json` – created only when a file cannot be read; the file is moved aside instead of being overwritten

When saving, the application first writes the complete data to a temporary file, then moves the previous file to the backup location, and only afterwards moves the new file into place. This prevents an interrupted save operation from leaving behind an incomplete data file.

If the main file is corrupted, the application automatically loads the backup when starting and informs the user.

The exact storage path is displayed under **Settings**.

## Running Locally

Requirement: **JDK 21** or newer. Maven is provided through the included Maven Wrapper, so a separate Maven installation is not required.

```bash
# Windows
mvnw.cmd javafx:run

# macOS and Linux
./mvnw javafx:run
```

If `java -version` shows an older version, switch to JDK 21:

```bash
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"

# macOS and Linux
export JAVA_HOME=/path/to/jdk-21
```

## Tests

```bash
./mvnw test
```

The tests cover rounding, including the boundary cases 4.24 / 4.25 / 4.74 / 4.75, weighting, input validation, saving and loading, backups, and recovery from corrupted files.

## Building Installers Locally

`jpackage` creates a native installer for the operating system currently in use. Each installer includes its own Java runtime created with `jlink`. **Users do not need to install Java separately.**

An installer can only be built on its target platform – for example, a Windows installer must be built on Windows.

```bash
./mvnw -Pinstaller clean package
```

The resulting files are located in `target/installer/`:

| Operating System | File | Additional Requirements |
|---|---|---|
| Windows | `Notenrechner-1.0.0.msi` (Start Menu and desktop shortcuts) | [WiX Toolset 3.x](https://github.com/wixtoolset/wix3/releases) |
| macOS | `Notenrechner-1.0.0.dmg` | – |
| Linux | `notenrechner_1.0.0_amd64.deb` | `fakeroot`, `binutils` |

The application icon is provided as a red-and-white placeholder in `packaging/`. It can be regenerated using `java packaging/IconGenerator.java`. To use a custom icon, either replace the files or modify the generator.

## Creating a Release on GitHub

The workflow `.github/workflows/release.yml` builds installers for `windows-latest`, `macos-latest`, and `ubuntu-latest` whenever a version tag is pushed. The installers are then attached to the corresponding GitHub Release.

```bash
git tag v1.0.0
git push origin main
git push origin v1.0.0
```

Afterwards, a new entry is automatically created under *Releases*, containing the `.msi`, `.dmg`, and `.deb` installers.

For testing purposes, the workflow can also be started manually via *Actions → Release → Run workflow*. In this case, no GitHub Release is created, but the installers are available as downloadable workflow artifacts.

The version number is defined in `pom.xml` under `<version>` and must match the Git tag.

## Warnings for Unsigned Installers

The installers are not signed with a certificate. This is not an error; it is the normal situation when no paid code-signing certificate is available.

As a result, both operating systems may display a security warning:

- **Windows (SmartScreen):** "Windows protected your PC."
  → Select *More info* → *Run anyway*.

- **macOS (Gatekeeper):** "Notenrechner cannot be opened because the developer cannot be verified."
  → In Finder, right-click the application → *Open* → confirm by selecting *Open* again. Alternatively, go to *System Settings → Privacy & Security → Open Anyway*.

- **Linux:** No warning.

To avoid these warnings, a code-signing certificate is required for Windows, while macOS requires an Apple Developer ID and notarization.

## Project Structure

```text
src/main/java/ch/notenrechner/
├── NotenrechnerApp.java        Application entry point, connects all components
├── model/                      Data model with JavaFX properties
│   ├── GradeBook, Semester, CourseModule, Grade
├── service/                    Business logic, without UI dependencies
│   ├── GradeCalculator         Average and rounding (BigDecimal)
│   ├── GradeScale              Grading scale 1.0–6.0
│   ├── GradeValidator          Input validation with user-friendly messages
│   ├── GradeBookService        Create, update, and delete operations
│   └── AutoSaveService         Automatic saving
├── persistence/                Data storage behind an interface
│   ├── GradeRepository         Repository interface
│   ├── JsonGradeRepository     JSON storage, atomic writes, backups
│   ├── AppPaths                OS-specific data directories
│   ├── GradeBookMapper         Model ↔ file mapping
│   ├── SchemaMigrator          Migration of older file formats
│   └── dto/                    Serialization objects (records)
└── ui/                         FXML, controllers, and helper classes
    ├── MainController          Sidebar and view navigation
    ├── Views                   List of views (extension point)
    ├── view/                   The four application views
    ├── chart/GradeBarChart     Bar chart
    ├── control/                Grade scale and dialogs
    └── util/                   Formatting, icons, theme
```

```text
src/main/resources/ch/notenrechner/
├── css/theme.css               All colors in one place
├── css/app.css                 Application styling
├── ui/*.fxml                   Layout and views
└── icon/                       Application icon
```

The separation of responsibilities is strict: `model` and `service` have no knowledge of the JavaFX UI, `persistence` only knows about the model, and neither the Java nor FXML code contains any inline styling. The entire interface is styled exclusively through CSS classes and stylesheets.

Design decisions are documented in [DESIGN.md](DESIGN.md), while product specifications are documented in [PRODUCT.md](PRODUCT.md).

## Extending the Application

### Adding a New View

The sidebar is generated from a list. Adding another view requires only three steps and does not require changes to the existing architecture:

1. **Create an FXML file** under `src/main/resources/ch/notenrechner/ui/view/`, for example `statistics-view.fxml`. Use `overview-view.fxml` as a template: a `page-header` at the top, followed by the content.

2. **Create a controller** in `ch.notenrechner.ui.view` that implements `ViewController`:

   ```java
   public final class StatisticsController implements ViewController {
       @Override
       public void init(AppServices services) {
           // services.gradeBook() provides the service containing the model
       }
   }
   ```

3. **Add an entry** to `ch.notenrechner.ui.Views#entries()`:

   ```java
   
    new NavigationEntry("statistics", "Statistics", Icons.CHART,
BASE + "statistics-view.fxml", StatisticsController::new)
   ```

The sidebar button, loading, view state preservation, and navigation are generated automatically. If the controller requires a new icon, add the SVG path to `ui/util/Icons`.

### Changing the Colors

All colors are defined as variables in:

`src/main/resources/ch/notenrechner/css/theme.css`

Simply change the values there. No color values are hardcoded anywhere else in the code.

### Changing the Storage Implementation

`GradeRepository` is the only interface through which the application interacts with its storage system.

To implement SQLite storage, simply create a second implementation of this interface. In `NotenrechnerApp`, replace `new JsonGradeRepository()` with the new class. The UI and services remain unchanged.

### Extending the File Format

Increase `GradeBookDto.CURRENT_SCHEMA_VERSION` and add a migration step to `SchemaMigrator#migrate` that converts data from the previous version.

Files with a higher schema version than the installed application supports are deliberately not opened. This prevents older application versions from accidentally losing data.
