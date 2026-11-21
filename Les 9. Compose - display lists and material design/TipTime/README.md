# Tip Time

Huiswerk les 9: Android Basics with Compose, Unit 2, pathway 3 ("Interact with UI and state"). Een app die de fooi
berekent uit het bedrag van de rekening en een percentage, met een schakelaar om naar boven af te ronden.

Gebaseerd op de codelabs "Intro to state in Compose", "Calculate a custom tip" en "Write automated tests".

## Openen en draaien

1. Open in Android Studio (of IntelliJ IDEA met de Android-plugin) via **File > Open** deze map `TipTime`.
2. Wacht tot Gradle klaar is met synchroniseren. Vraagt de IDE om een Android SDK of een update van de
   Android Gradle Plugin, volg dan de melding.
3. Kies een emulator of telefoon en klik op **Run 'app'**.

## Testen

| Soort | Bestand | Draaien |
|---|---|---|
| Unittests (4) | `app/src/test/.../TipCalculatorTests.kt` | Rechtsklik op het bestand > Run. Geen emulator nodig. |
| UI-tests (3) | `app/src/androidTest/.../TipUITests.kt` | Rechtsklik op het bestand > Run, met een emulator of telefoon aan. |

## Wat er anders is dan in de codelab

- De app is tweetalig: Engels (`values/strings.xml`) en Nederlands (`values-nl/strings.xml`).
- De invoer blijft bewaard bij het draaien van het scherm (`rememberSaveable` in plaats van `remember`).
- De UI-tests lezen hun teksten uit de string-resources, zodat ze ook slagen als de telefoon op Nederlands staat.
- Er zijn vier unittests en drie UI-tests in plaats van één van elk.
- De preview toont het scherm in het Engels en in het Nederlands naast elkaar.

## Gebruik van AI

De code is gegenereerd met Claude (Anthropic) op basis van de codelabs. Nog zelf te doen: de code doorlopen,
de app bouwen en draaien, en beide testsoorten uitvoeren.
