# Design

Visuelle Entscheide für **Notenrechner**. PRODUCT.md hält die Produktwahrheit,
diese Datei die durable visuelle Sprache. Modus: **Operate** – die Person ist in
einer Aufgabe, die Oberfläche tritt hinter die Zahl zurück.

## Welt

Schweizer Signaletik: die Nüchternheit von Fahrplänen und Formularen, Rot als
einzige laute Farbe, viel Weissraum, Zahlen in klarer Tabellen-Ausrichtung. Die
Welt liefert genau vier Dinge – Typografie, Palette, Dichte und einen
Signature-Move. Navigation und Bedienelemente bleiben die Standardelemente der
Plattform.

### Signature-Move: das Notenband

Ein schmales Band, das die Skala 1–6 als Strecke zeigt: Teilstriche bei jeder
ganzen Note, die Genügend-Grenze bei 4.0 als kräftige Markierung, die erreichte
Note als gefüllter Abschnitt in der Notenfarbe.

Das Band erscheint in jeder Modulzeile der Übersicht und wiederholt dieselbe
Geometrie, die das Säulendiagramm senkrecht verwendet. Dadurch liest sich eine
Modulnote überall gleich, und der Status hängt nicht allein an der Farbe,
sondern an der Position auf der Strecke. Das ist zugleich die Antwort auf
Rot-Grün-Sehschwäche.

## Palette

Zentral in `src/main/resources/ch/notenrechner/css/theme.css` als
JavaFX-Looked-up-Colors. Kein Farbwert steht irgendwo sonst im Projekt.

| Rolle | Variable | Wert | Einsatz |
|---|---|---|---|
| Markenrot | `-nr-red` | `#d52b1e` | Primäraktionen, Fokus, aktive Markierung |
| Markenrot dunkel | `-nr-red-strong` | `#b32319` | Hover auf Primäraktionen |
| Navigationsrot | `-nr-red-deep` | `#8f1710` | Seitenleiste (Weiss darauf: 9.4:1) |
| Grund | `-nr-bg` | `#f4f2f1` | Fensterhintergrund, warmes Grau |
| Fläche | `-nr-surface` | `#ffffff` | Karten, Tabellen |
| Linie | `-nr-border` | `#e3dedb` | Trennlinien, Kartenrand |
| Text | `-nr-text` | `#1c1917` | Fliesstext, Zahlen |
| Text leise | `-nr-text-muted` | `#6c6360` | Beschriftungen, Sekundärinfo |

Notenfarben – sie bedeuten ausschliesslich Notenwerte und sind von der
Markenfarbe getrennt zu lesen:

| Note | Variable | Wert |
|---|---|---|
| unter 4.0 | `-nr-grade-insufficient` | `#c62828` |
| 4.0 | `-nr-grade-40` | `#9ccc95` |
| 4.5 | `-nr-grade-45` | `#6fb869` |
| 5.0 | `-nr-grade-50` | `#45a049` |
| 5.5 | `-nr-grade-55` | `#2e7d32` |
| 6.0 | `-nr-grade-60` | `#1b5e20` |

Die Grünstufen steigen gleichmässig in Sättigung und Dunkelheit, sodass die
Reihenfolge auch in Graustufen erkennbar bleibt.

## Typografie

Eine Familie, Systemschrift der jeweiligen Plattform – in einem Werkzeug ist
Vertrautheit eine Qualität, und der Installer bleibt ohne mitgelieferte Schrift
schlank.

```
"Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Noto Sans", sans-serif
```

Feste Stufen (kein fluides Skalieren), Verhältnis ca. 1.2:

| Rolle | Grösse | Gewicht |
|---|---|---|
| Seitentitel | 26 px | 600 |
| Kartentitel | 17 px | 600 |
| Fliesstext | 14 px | 400 |
| Beschriftung | 12.5 px | 500 |
| Mikro-Beschriftung | 11 px | 600, `letter-spacing: 0.06em`, Versalien |
| Notenzahl gross | 30 px | 600, Tabellenziffern |
| Notenzahl Zeile | 19 px | 600, Tabellenziffern |

Zahlen stehen in Tabellen und Listen rechtsbündig und in Spalten fester
Breite, damit sie untereinander fluchten.

## Dichte und Raster

Abstandsskala: 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 px. Über einer Überschrift
steht mehr Luft als darunter. Radien: Karte 12, Bedienelement 8, Pille 999.
Schatten immer mit Versatz nach unten und weicher Streuung, nie als farbiger
Hof: `dropshadow(gaussian, rgba(28,25,23,0.07), 18, 0, 0, 4)`.

## Shell

- **Seitenleiste**, 236 px fix, Navigationsrot, Weiss darauf. Oben die
  Wortmarke, darunter vier Einträge, unten der Speicherzustand. Aktiver Eintrag:
  weisse Pille mit rotem Text – die Umkehrung macht ihn auch ohne Farbsehen
  eindeutig.
- **Arbeitsfläche** auf `-nr-bg`, mit eigenem Kopfbereich (Titel, einzeiliger
  Erklärsatz, Primäraktion rechts) und darunter dem scrollbaren Inhalt.
- Vier Ansichten: Übersicht (Start), Semester & Module, Diagramm, Einstellungen.
  Die Navigation wird aus einer Liste erzeugt; eine fünfte Ansicht ist ein
  weiterer Listeneintrag, kein Umbau.

## Zustände

Jedes Bedienelement hat Normal, Hover, Fokus, Aktiv und Deaktiviert. Fokus immer
sichtbar: 2 px Ring in `-nr-red`, mit 2 px Abstand zum Element.

JavaFX 21 kennt keine CSS-Übergänge, deshalb wechseln Hover- und Fokuszustände
sofort – auf dem Desktop ohnehin das erwartete Verhalten. Der eine gestaltete
Moment ist der Ansichtswechsel: 170 ms Einblenden mit 8 px Aufwärtsbewegung,
exponentiell auslaufend.

Leere Zustände erklären den nächsten Schritt und bieten ihn direkt an:

- Keine Semester: geführter Einstieg, ein Klick legt das erste Semester an.
- Semester ohne Module, Modul ohne Noten: je ein Satz plus die passende Aktion.
- Modul ohne Noten zeigt in jeder Auswertung «–», nie 0.0.

## Ikonografie

Vier selbst gezeichnete SVG-Pfade auf 24er-Raster, durchgehend gefüllt, gleiche
Rundung (1.5 px). Keine Emoji, keine Unicode-Zeichen als Symbolersatz.

## Was hier nicht hingehört

- Kacheln mit grosser Zahl und kleinem Label als Seitengerüst.
- Verlaufstext, Glas-Effekte, farbige Balken links an Karten.
- Farbe als einziger Statusträger: neben jeder Notenfarbe steht die Zahl und
  ein Wort.
