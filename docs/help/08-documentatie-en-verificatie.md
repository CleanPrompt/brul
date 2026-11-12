# Stap 8: Documentatie & Verificatie

In deze finale stap ronden we de Help-feature professioneel af. We zorgen voor een formele **OpenAPI 3.0 specificatie**, een interactief **IntelliJ HTTP Client testscript** (`.http`), backend README's en een sluitend verificatierapport.

---

## 1. Doel van deze Stap

Een backend-feature is pas écht "Production Ready" als aan drie voorwaarden is voldaan:
1. **Automated Testing:** 100% van de unit-, database- en route-tests slagen.
2. **Machine-Readable Contract:** Externe consumenten (zoals frontend apps of teamgenoten) hebben een eenduidige specificatie van alle endpoints, request bodies en response types via OpenAPI/Swagger.
3. **Exploratory & Smoke Testing:** Ontwikkelaars kunnen de API live testen met één klik via `.http` bestanden in hun IDE, inclusief geautomatiseerde assertions op de responses.

---

## 2. OpenAPI 3.0 Specificatie (`openapi.yaml`)

In `backend/resources/openapi/openapi.yaml` zijn alle Help-endpoints en datastructuren formeel gedocumenteerd:

### Toegevoegde Endpoints
- `GET /api/v1/help-requests/{id}/calendar.ics`: Publiek downloaden van een RFC 5545 iCalendar stream.
- `GET /api/v1/help-requests/{id}/participants/summary`: Publiek capaciteitsoverzicht (hoeveel vrijwilligers gezocht, hoeveel aangemeld, hoeveel plekken nog vrij).
- `GET /api/v1/help-requests/{id}/participants`: Lijst van aangemelde vrijwilligers (JWT geauthenticeerd).
- `POST /api/v1/help-requests/{id}/participants`: Aanmelden als vrijwilliger (JWT geauthenticeerd, `201 Created` of `409 Conflict`).
- `DELETE /api/v1/help-requests/{id}/participants/me`: Afmelden als vrijwilliger (JWT geauthenticeerd, `204 NoContent`).
- `POST /api/v1/help-requests/{id}/check-in-code`: Genereren van een 6-cijferige OTP check-in code door de eigenaar (JWT geauthenticeerd).
- `POST /api/v1/help-requests/{id}/check-in`: Aanwezigheid registreren door de vrijwilliger met OTP code en GPS coördinaten (JWT geauthenticeerd).

### Toegevoegde Schemas
- `HelpParticipationResponse`: DTO representatie van een aanmelding met status (`SIGNED_UP`, `ATTENDED`, etc.).
- `HelpParticipationSummaryResponse`: Samenvatting van de capaciteit en beschikbaarheid.
- `CheckInRequest`: Invoer met OTP code en optionele GPS breedte- en lengtegraad (`latitude`, `longitude`).
- `CheckInResponse`: Resultaat van de incheckpoging (`success: true/false`, `checkedInAt`).
- `CheckInCodeResponse`: Actieve code en verloopdatum (`expiresAt`).

### Waarom is dit belangrijk voor het project?
- **TypeScript Generatie:** Frontends (zoals React of Vue) kunnen met één commando type-safe API-clients genereren.
- **Contract-first samenwerking:** Teamgenoten die aan de schermen werken weten exact welke statuscodes en JSON-structuren ze kunnen verwachten.
- **Geen aannames:** Foutcodes zoals `"ALREADY_PARTICIPATING"`, `"NOT_HELP_REQUEST_OWNER"` en `"LOCATION_TOO_FAR"` zijn expliciet vastgelegd.

---

## 3. IntelliJ HTTP Client (`backend/http/6-help.http`)

In plaats van logge tools als Postman gebruikt Brul native JetBrains `.http` bestanden. Deze bestanden worden direct in Git beheerd en kunnen door iedere ontwikkelaar worden uitgevoerd met de groene *Play*-knop in IntelliJ.

In `backend/http/6-help.http` hebben we een volledige smoke test ingericht:
1. **Download `.ics`:** Controleert of de response HTTP 200 is en `BEGIN:VCALENDAR` bevat.
2. **Capaciteit samenvatting:** Controleert of numerieke velden correct worden geretourneerd.
3. **Aanmelden als vrijwilliger:** Voert een POST uit met het `userToken`.
4. **Deelnemerslijst opvragen:** Controleert of het resultaat een JSON array is.
5. **OTP Code genereren als eigenaar:** Slaat de gegenereerde 6-cijferige code direct op in een omgevingsvariabele (`client.global.set("activeCheckInCode", ...)`).
6. **Inchecken:** Gebruikt de opgeslagen code en stuurt GPS-coördinaten mee om de aanwezigheid te bevestigen.
7. **Afmelden:** Ruimt de aanmelding netjes op via een `DELETE` request.

---

## 4. Totaaloverzicht van de Verificatie

Tijdens de 8 stappen is een uitgebreid vangnet van unit-, integratie- en database-tests opgebouwd:

| Stap | Onderdeel | Test Suite | Aantal Tests | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Stap 2** | Pure Logica & Value Objects | `ParticipationStatusTest` | 3 | ✅ Geslaagd |
| **Stap 2** | Pure Logica & Value Objects | `CheckInValidatorTest` | 9 | ✅ Geslaagd |
| **Stap 2** | Pure Logica & Value Objects | `CalendarFeedServiceTest` | 3 | ✅ Geslaagd |
| **Stap 3** | Domein & DTO Mappers | `HelpParticipationMapperTest` | 3 | ✅ Geslaagd |
| **Stap 5** | Repository & H2 Database | `HelpParticipationRepositoryTest` | 6 | ✅ Geslaagd |
| **Stap 6** | Service & Autorisatie Regels | `HelpParticipationServiceTest` | 17 | ✅ Geslaagd |
| **Stap 7** | HTTP Routes & Ktor Endpoints | `HelpParticipationRoutesTest` | 8 | ✅ Geslaagd |
| **Bestaand** | Auth, Users, Roles, Feedback | Bestaande suites | 127 | ✅ Geslaagd |
| **Totaal** | **Project Test Run** | `kotlin.bat test` | **176 tests** | **100% Geslaagd** |

---

## 5. Hoe te Demonsteren aan Docenten of Medestudenten

Wil je laten zien hoe de Help-feature werkt? Volg deze stappen:

1. **Draai de tests:**
   ```bash
   .\kotlin.bat test
   ```
   *Resultaat:* Alle 176 tests draaien in ~40 seconden groen.
2. **Start de applicatie:**
   ```bash
   .\kotlin.bat run
   ```
   *Resultaat:* Ktor Netty server start op poort 8080 en initialiseert de H2 database en tabellen (`help_participations`).
3. **Voer de HTTP Smoke Test uit:**
   - Open IntelliJ IDEA.
   - Voer `backend/http/1-authentication.http` uit (logt in als admin en reguliere gebruiker).
   - Voer `backend/http/6-help.http` uit.
   - Zie alle groene vinkjes in het *Services / HTTP Client* tabblad van IntelliJ!
