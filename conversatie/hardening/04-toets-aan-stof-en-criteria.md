# Toets aan de lesstof en de criteria (periode 1)

Datum: 2026-11-17. Bronnen: de leeruitkomst LU 1, de eisen aan de proftaak, de checklist voor portfolio deel 1 en de
lesstof van les 1 tot en met 7 (uitgeschreven slides). Context: hbo-informatica jaar 2, eerste keer Kotlin en Ktor.

01 en 03 kijken naar de code zelf. Dit bestand kijkt naar **wat de beoordelaar wil zien**, en wat de Base nog mist.

---

## 1. Samenvatting

De Base is technisch sterker dan de lesstof vraagt (RBAC uit de database, tokenversie, paging, 158 tests). De gaten
zitten niet in de code, maar **eromheen**:

| Gat | Waarom het telt | Ernst |
|---|---|---|
| **Geen demodata** | Eis: "bij het assessment bevat de database al realistische data", "de docent kan de applicatie zelf testen" | Hoog, maar pas na reports (§4) |
| **OO-abstractie in het domein ontbreekt** (geen abstracte klasse, geen eigen overerving in het domein) | Eis: klassendiagram met overerving, interfaces en abstracte klasse; "3 soortgelijke klassen met gedeelde functionaliteit" | Hoog, oplossen met reports |
| **UML voor het ontwikkeldocument ontbreekt** (use case, klasse, package, 3× sequence) | Verplicht onderdeel van portfolio deel 1 | Hoog |
| **Geen testrapport** | "Testrapportage van alle relevante API-requests" | Middel |
| **Kotlin-concepten niet aanwijsbaar gemaakt** | Assessment: "in je eigen code kunnen aanwijzen waar je iets gebruikt" | Middel |
| **Bijdrage per student en Git-samenwerking** | Elke student minstens 3 eigen unittests; filmpje laat Git-samenwerking zien | Hoog: tot nu toe heeft één persoon alles gebouwd (§6) |

---

## 2. Het report-model

De eis "3 soortgelijke klassen met gedeelde functionaliteit" en de OO-abstractie in het klassendiagram worden ingevuld
door reports: `sealed class Report` met `SafetyReport`, `NuisanceReport` en `HelpReport`, in één tabel. Uitgewerkt
in `conversatie/reports/02-advies-uit-hardening.md` §1.

---

## 3. De eisen naast de Base gelegd

### Functionele eisen

| Eis | Base nu | Na reports |
|---|---|---|
| 3 soortgelijke klassen met gedeelde functionaliteit | Nee (feedback is één klasse) | Ja, `Report` + 3 subklassen |
| Aanmaken, collectie bekijken, zoeken/filteren, verwijderen | Ja, bij `feedback` en `users` | Ja |
| Lezen en schrijven naar een database | Ja, H2 met Exposed | Ja |
| **Realistische data bij het assessment** | **Nee**: alleen rollen en permissies worden geseed | Demodata nodig (§4) |

### Niet-functionele eisen

| Eis | Base nu |
|---|---|
| Web API in Kotlin met Ktor | Ja |
| Code en documentatie in het Engels | Ja (alleen `conversatie/` is Nederlands, buiten het product) |
| Kotlin-conventies, zinvolle packagenamen | Ja: `features/<naam>/route\|service\|repository\|...` |
| Zinnig gebruik van OO-principes | Deels: interfaces + `sealed class AppException`; domein-overerving komt met reports |
| Ontwerpprincipes en patterns onderbouwd | Ja in de code (repository pattern, dependency inversion, SRP); **nog niet beschreven** in het ontwikkeldocument |
| Draait op je eigen computer | Ja (`./kotlin run`) |
| ≥ 3 unit- en ≥ 3 integratietests | Ja (158) |
| Tests voor de 4 soorten CRUD-requests | Ja (`FeedbackRoutesTest`) |
| Elke student ≥ 3 eigen, diverse unittests | **Nog te plannen** (§6) |
| KDoc "precies voldoende" | Goed: de KDoc legt vooral het *waarom* uit. Niet meer toevoegen |

### Portfolio deel 1: ontwikkeldocument

| Onderdeel | Base nu | Actie |
|---|---|---|
| User stories | Nee | Schrijven voor de hele app (ook Android, periode 2) |
| API-documentatie | Ja, `openapi.yaml` | Bijhouden |
| Use case-diagram | Nee | Maken |
| Klassendiagram met OO-abstractie | Alleen een voorstel voor reports (`conversatie/reports/`, Nederlands) | Maken, Engels, inclusief Base |
| Packagediagram | Nee (wel de mappenstructuur in het root-README) | Maken |
| ≥ 3 sequence diagrams | 1 in `docs/guide/03-architecture-and-requests.md` | Drie voor het document (§5) |
| Testrapport van alle relevante requests | Nee | Maken (§6) |
| Installatie en gebruik | Ja, root-README | Demo-accounts toevoegen |
| Verantwoording AI-gebruik | Ja, `docs/ai-usage.md` (§7) | (to confirm)-punten invullen, bijhouden |
| Permissiematrix (les 3/4) | Ja, `TECHNICAL_DESIGN.md` | Uitbreiden met reports |

---

## 4. Demodata

De eis blijft (realistische data bij het assessment), maar de seeder komt **na reports**, als laatste stap. Nu zou hij
alleen gebruikers en feedback vullen en later grotendeels herschreven moeten worden. Plan en alternatieven:
`conversatie/reports/02-advies-uit-hardening.md` §5.

---

## 5. Diagrammen voor het ontwikkeldocument

Eis: use case-diagram, klassendiagram met OO-abstractie, packagediagram en minstens 3 sequence diagrams. Waar ze
komen (uitgewerkt in checkpoint 7 van de roadmap):

| Diagram | Waar | Waarom daar |
|---|---|---|
| Klasse, per feature | `## Class diagram` in de README van elke feature en van `shared/` | Eén bron per feit: de klassen van een feature horen bij die feature, en worden samen met de code bijgewerkt |
| Sequence 1: inloggen | README van `authentication` | Het belangrijkste verloop van die feature |
| Sequence 2: beveiligd verzoek met eigenaarscontrole | README van `feedback` | De referentie-feature, met de 401/403/404-takken |
| Sequence 3: profielfoto uploaden | README van `user` | Laat de controle vóór het lezen van de body zien |
| Package | `TECHNICAL_DESIGN.md` §2 | Gaat over alle features samen |
| Use case | `docs/`, in draw.io of PlantUML | Gaat over de hele app, ook Android; Mermaid kent geen echt use case-diagram |

Het Word-document neemt de diagrammen over als afbeelding en verwijst naar de bron.

---

## 6. Tests, testrapport en bijdrage per student

**Testrapport.** De eis is een rapport van "alle relevante API-requests". Voorstel: een tabel per endpoint (request,
verwachte status, resultaat, welke test of welk `.http`-request het aantoont), plus de samenvatting van
`./kotlin test` (aantal tests, geslaagd). Zet het in `docs/test-report.md`, zodat het met de code meegroeit.

**Bijdrage per student.** De eis "iedere student minimaal 3 zinvolle unittesten" wordt waarschijnlijk via Git
gecontroleerd, en het filmpje moet de Git-samenwerking laten zien. Stand op 2026-11-17: team van 5, tot nu toe heeft
één persoon alles gebouwd. Gepland: Kelvin bouwt de help-feature (vrijwilligers) na de basis van reports. De rol van
de andere drie is nog onbekend. Advies:

- Meld de teamsituatie nu bij de docent, feitelijk (wie deed wat, wat is afgesproken), niet pas bij het assessment.
- Laat teamgenoten al tijdens de hardening werk doen dat niet op reports wacht, bijvoorbeeld het use case-diagram,
  de user stories (checkpoint 7) of het testrapport (checkpoint 8). Zo leren ze de Base kennen.
- Verdeel reports in stukken, elk op een eigen branch met een pull request: CRUD + lijst, status, foto's, likes,
  vrijwilligers. Elke student schrijft de tests van zijn eigen stuk; de review staat in de pull request.
- Schrijf de taakverdeling eerlijk op; het ontwikkeldocument vraagt erom.

Dit is een advies over de **werkwijze**. Het ontwerp blijft vanuit `Base` komen.

---

## 7. Verantwoording AI-gebruik

Staat in **`docs/ai-usage.md`** (Engels, de bron voor het ontwikkeldocument): tools, werkwijze, verificatie,
voorbeelden van goede en bijgestuurde uitkomsten, lessons learned en een logboek. Nog in te vullen: de punten
**(to confirm)**.

---

## 8. Waar de Base afwijkt van de lesstof (verdedigen)

Bij het assessment kan de vraag komen "in de les deden we X, waarom jullie Y?". Elke afwijking staat al in de
beslissingentabel, behalve waar dat hieronder staat.

| Lesstof | Base | Verdediging |
|---|---|---|
| Rol als custom claim in de JWT (`userType`), `authenticate("staff")` | Permissies uit de database, alleen `sub` en `ver` in de token | Rol intrekken werkt meteen; de token bevat geen persoonsgegevens. Staat in de tabel *Security* |
| `receiveMultipart()` en bestanden op schijf, `staticFiles` | Ruwe bytes in de body, opgeslagen in de database | Eén request, geen bestanden die los van de database raken. Staat in de tabel *Images* |
| Afbeeldingen opruimen bij een DELETE (les 6, §47) | Komt met reports: `ON DELETE CASCADE` | Geen losse bestanden, dus de database ruimt het op. **Nog opschrijven** |
| `application.conf` met meerdere modules | `AppConfig` + `embeddedServer` | Staat in de tabel *Architecture* |
| Exposed DAO (`IntIdTable`, `by`-delegates) | Exposed DSL, UUID's | DSL leest als SQL, UUID's zijn niet te raden. **Alternatief DAO nog opschrijven** |
| JUnit 5, `@ParameterizedTest` | Kotlin Test | Standaard van de Kotlin-toolchain (les 3) |
| `kotlinx.datetime` aangeraden (les 1/3) | `java.time.Instant` | Exposed `exposed-java-time` werkt met `Instant`; geen extra dependency. **Nog opschrijven** |
| Koin in het voorbeeldproject (les 7) | Handmatige constructor injection | Staat in de tabel *Architecture* |

---

## 9. Kotlin-concepten aanwijsbaar maken

De slides van les 7 zeggen: kunnen aanwijzen waar je in je eigen code visibility, null safety, collections
(filter/map/group/sort), interfaces, overerving, overloading, overriding, named parameters en generics gebruikt.

Telling in `backend/src` (2026-11-17):

| Concept | Aanwezig | Voorbeeld |
|---|---|---|
| interface + implementaties (polymorfisme) | Ja | `UserRepository` → `ExposedUserRepository`, `FakeUserRepository` |
| `sealed class`, overerving, exhaustive `when`, `is` | Ja | `AppException` en `toHttpStatus()` in `StatusPages.kt` |
| `data class` vs. `class`, gelijkheid | Ja | `User` vs. `ProfileImage` (`ByteArray` vergelijkt op referentie) |
| `object`, `companion object` | Ja | `UsersTable`, `DatabaseFactory`; `ImageType.MAX_BYTES` |
| extension functions, top-level functions | Ja, veel | mappers, `Parameters.page()`, `containsIgnoringCase` |
| null safety `?.`, `?:`, `?.let` | Ja | `Authentication.kt` (`validate`), `QueryParameters.kt` |
| higher-order function, function type, trailing lambda | Ja | `requireValid(condition) { "..." }` (`inline`, `() -> String`) |
| lambda with receiver | Ja | Ktor-DSL (`routing { }`), `transaction { }` |
| scope functions | Deels | `let`, `takeIf`; geen `apply`/`also` in `src` |
| generics | Ja | `PageResponse<T>`, `oneOf<T>` |
| named en default arguments | Ja | `AppException.BadRequest(message, code = ...)`, `AuthenticationService(...)` in `Application.kt` |
| visibility | Ja | `private fun`, `private companion object` |
| `Nothing` | Impliciet | `error(...)` en `throw` in een elvis |
| collections `associateBy` | Ja | `FeedbackQueryMapper` |
| `groupBy`, `sortedBy` | Nee | — |
| abstracte klasse | Nee | komt met `Report` (§2) |
| `init`-blok | Nee | komt met `Location` (reports) |
| delegated property (`by`) | Nee | — |
| overloading | Nee | — |

**Advies:**

1. Vul de laatste rijen **niet** met gekunstelde code. Abstracte klasse en `init` komen vanzelf met reports. Wat
   daarna nog ontbreekt (delegated property, overloading), toon je in het **individuele** portfolio (Hyperskill,
   workshops). Dat is daar ook voor bedoeld.
2. Zet een Engelse tabel *Kotlin concepts in this project* (concept → bestand) in `TECHNICAL_DESIGN.md`. Het is een
   lijst, dus daar hoort hij volgens "één taak per document". Bij het assessment wijs je dan direct aan.

---

## 10. AI-agent (Koog)

**Besloten (2026-11-17): niet nodig.** LU 1 noemt een AI-agent, maar volgens de modulebeschrijving is het optioneel,
en het team kiest ervoor het niet te bouwen. Geen nieuwe dependency.
