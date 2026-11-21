# Unit 2 Apps

Huiswerk les 9: de overige apps van Android Basics with Compose, Unit 2. De Tip Calculator staat apart in `TipTime`.

Eén project met drie apps (modules). Elke app heeft zijn eigen run-configuratie en wordt als losse app op de
telefoon of emulator geïnstalleerd.

| Module | Codelab | Wat de app doet |
|---|---|---|
| `diceroller` | Pathway 2: Create an interactive Dice Roller app | Gooit een dobbelsteen en toont het resultaat |
| `lemonade` | Pathway 2: Practice: Click behavior | Limonade maken in vier stappen: plukken, persen, drinken, opnieuw |
| `artspace` | Pathway 3: Project: Create an Art Space app | Kunstgalerij met Vorige- en Volgende-knoppen |

## Openen en draaien

1. Open deze map `Unit2Apps` via **File > Open** en wacht tot Gradle klaar is met synchroniseren.
2. Kies bovenaan de run-configuratie `diceroller`, `lemonade` of `artspace`, kies de emulator en klik op **Run**.

## Testen

| App | Unittests (`src/test`) | UI-tests (`src/androidTest`) |
|---|---|---|
| Dice Roller | 3: altijd 1 t/m 6, elke kant kan vallen, zelfde seed geeft zelfde worp | |
| Lemonade | 4: de stappen, het aantal keer persen, terug naar het begin | |
| Art Space | 4: volgende, vorige en het rondgaan aan het begin en eind | 3: start, Volgende, Vorige |

Unittests: in het Gradle-paneel onder `verification` dubbelklikken op `test`. UI-tests: emulator aan en
`connectedDebugAndroidTest` (alleen Art Space heeft UI-tests).

## Wat er anders is dan in de codelabs

- Alle drie de apps zijn tweetalig: Engels en Nederlands (`values-nl/strings.xml`).
- De stand blijft bewaard bij het draaien van het scherm (`rememberSaveable`).
- De logica (dobbelsteen gooien, de volgende limonadestap, het volgende kunstwerk) staat in losse functies,
  zodat die met unittests te testen is zonder emulator.
- De afbeeldingen (dobbelsteen, citroenen, kunstwerken) zijn eenvoudige vectortekeningen in plaats van de
  afbeeldingen uit de codelabs. Vervang de kunstwerken in Art Space gerust door eigen foto's of tekeningen.
