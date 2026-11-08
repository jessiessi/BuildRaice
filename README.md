# BuildRaice

Ein Bau-Wettbewerb als Paper-Plugin: Alle bekommen dasselbe Thema und eine feste Zeit, danach werden
die Bauwerke reihum bewertet. Gespielt werden beliebig viele Runden, die Punkte werden zusammengezählt.

## Ablauf

1. **Box:** Beim Join landen alle in einer Glasbox in der Bauwelt.
2. **Bauen:** Ein OP startet die Runde mit `/build <Thema> <Minuten>`. Jeder bekommt einen eigenen
   Plot (32×32 Blöcke) im Creative-Modus, um die Plots steht eine Wand und die Bossbar zeigt Thema
   und Restzeit. Mit dem roten Farbstoff lässt sich der Boden des Plots ändern.
3. **Bewertung:** Wenn die Zeit um ist, besuchen alle gemeinsam die bebauten Plots in zufälliger
   Reihenfolge. Wem ein Plot gehört, bleibt bis zum Ergebnis geheim. Mit den Tasten 3–7 oder dem
   Mausrad wählt man eine Farbe, ein Linksklick loggt sie ein:

   | Taste | Block | Wertung | Punkte |
   |---|---|---|---|
   | 3 | Rot | Schlecht | 1 |
   | 4 | Orange | Geht so | 2 |
   | 5 | Gelb | Ok | 3 |
   | 6 | Hellgrün | Super | 4 |
   | 7 | Grün | Wunderschön | 5 |

4. **Ergebnis:** Der Sieger-Plot wird mit Feuerwerk gefeiert, danach geht es zurück in die Box und die
   Plots werden geleert. Die Punkte stehen in der Sidebar.
5. **Ende:** `/ende` beendet das Spiel und öffnet allen die Bestenliste.

Leere Plots werden bei der Bewertung übersprungen, ebenso Spieler, die nur zuschauen.

## Befehle

| Befehl | Wer | Was |
|---|---|---|
| `/build <Thema> <Minuten>` | OP | Startet eine Runde |
| `/stop` | OP | Bricht die laufende Runde ab, sie zählt nicht (in der Konsole fährt `stop` den Server herunter) |
| `/skip` | OP | Überspringt den gerade bewerteten Plot, er bekommt keine Punkte |
| `/ende` | OP | Beendet das Spiel und zeigt die Bestenliste |
| `/bestenliste` | alle | Öffnet die Bestenliste |
| `/spectate` | alle | Nur zuschauen statt mitspielen, bis man `/spectate` erneut eingibt |
| `/plot`, `/plot info` | alle | Zum eigenen Plot teleportieren bzw. Plot-Infos anzeigen |

## Voraussetzungen und Bauen

- Paper 26.3
- Java 25

```bash
./gradlew build        # Plugin-Jar in build/libs/
./gradlew runServer    # lokaler Testserver im Ordner run/
```
