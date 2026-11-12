# Stap 7: HTTP Routes & Ktor Wiring

In deze stap ontsluiten we alle functionaliteit van de Help-feature via het HTTP REST API protocol met behulp van **Ktor**. We koppelen de services en repositories in `Application.kt` en `TestFixtures.kt`, en bouwen een volledige set integratietests met Ktor's `testApplication`.

---

## 1. Doel van deze Laag

De HTTP Route-laag is de *buitenste schil* van onze feature. Deze laag heeft slechts een paar specifieke verantwoordelijkheden:
1. **HTTP Verzoek ontleden:** Het uitlezen van pad-parameters (zoals `{id}`), query parameters, headers (zoals `Authorization: Bearer <token>`) en JSON request bodies (zoals `CheckInRequest`).
2. **Delegeren naar de Service-laag:** De route bevat **geen** domeinlogica, geofencing-berekeningen of SQL-queries. Hij roept direct de `HelpParticipationService` aan.
3. **HTTP Antwoord construeren:** Het resultaat transformeren naar een representatie (DTO JSON of `.ics` plain text) en de juiste HTTP statuscode teruggeven (bijv. `200 OK`, `201 Created`, of `204 NoContent`).

---

## 2. Overzicht van de Endpoints

| Methode | Pad | Toegang | Beschrijving |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/help-requests/{id}/calendar.ics` | **Publiek** | RFC 5545 iCalendar bestand downloaden voor agenda apps |
| `GET` | `/api/v1/help-requests/{id}/participants/summary` | **Publiek** | Beschikbaarheid & capaciteitsoverzicht (hoeveel plekken vrij) |
| `GET` | `/api/v1/help-requests/{id}/participants` | **JWT Auth** | Lijst van aangemelde vrijwilligers bekijken |
| `POST` | `/api/v1/help-requests/{id}/participants` | **JWT Auth** | Jezelf als vrijwilliger aanmelden (`201 Created`) |
| `DELETE` | `/api/v1/help-requests/{id}/participants/me` | **JWT Auth** | Jezelf afmelden als vrijwilliger (`204 NoContent`) |
| `POST` | `/api/v1/help-requests/{id}/check-in-code` | **JWT Auth** (Eigenaar) | 6-cijferige OTP check-in code genereren |
| `POST` | `/api/v1/help-requests/{id}/check-in` | **JWT Auth** (Vrijwilliger) | Aanwezigheid registreren met code & geolocatie |

---

## 3. Belangrijke Ktor Concepten & Code Uitleg

### 3.1 Publieke vs Beveiligde Routes
In `Routing.kt` splitsen we routes op basis van authenticatie:
- **Publiek:** `helpParticipationPublicRoutes(helpParticipationService)` hangt direct onder `/api/v1`. Kalendersynchronisatie moet vaak zonder login werken omdat externe agendaprogramma's (Google Calendar, Apple Agenda) via een URL subscriben.
- **Beveiligd:** `helpParticipationRoutes(helpParticipationService, authorizationService)` hangt binnen `authenticate(JWT_AUTHENTICATION)`. Hier controleert Ktor automatisch het JWT Bearer token. Als het token ontbreekt of ongeldig is, reageert Ktor direct met `401 Unauthorized`.

### 3.2 Veilige Parameters & Helpers
- `call.uuidParameter("id")`: Leest de `{id}` parameter uit het URL-pad. Is dit geen geldige UUID, dan gooit deze helper direct een `AppException.BadRequest`, wat door de `StatusPages` plugin wordt omgezet in een gestructureerde JSON foutmelding.
- `call.currentUserId()`: Haalt de geverifieerde `UUID` van de ingelogde gebruiker op uit het JWT `Principal` object. De controller hoeft dit niet zelf te parsen.

### 3.3 Aangepaste Content-Types (`calendar.ics`)
Voor het agendabestand sturen we geen JSON terug, maar specifieke iCalendar headers:
```kotlin
call.response.header(
    HttpHeaders.ContentDisposition,
    "attachment; filename=\"help-request-$id.ics\""
)
call.respondText(
    text = icsContent,
    contentType = ContentType.parse("text/calendar; charset=utf-8"),
    status = HttpStatusCode.OK
)
```
Hierdoor herkent de browser of telefoon direct dat het om een downloadbaar agendabestand gaat.

---

## 4. Bedrading (Wiring) zonder Dependency Injection Framework

In Brul gebruiken we **Constructor Injection** in plaats van zware reflectie-frameworks zoals Spring of Koin. Dit maakt de applicatie razendsnel om op te starten en 100% type-safe:

In `Application.kt`:
```kotlin
val helpParticipationRepository = ExposedHelpParticipationRepository()
val helpRequestProvider = InMemoryHelpRequestProvider()
val helpParticipationService = HelpParticipationService(helpParticipationRepository, helpRequestProvider)

configureRouting(
    ...,
    helpParticipationService
)
```

In `TestFixtures.kt`:
```kotlin
class TestServices {
    ...
    val helpParticipationRepository = FakeHelpParticipationRepository()
    val helpRequestProvider = FakeHelpRequestProvider()
    val helpParticipationService = HelpParticipationService(helpParticipationRepository, helpRequestProvider)
}
```
Doordat `HelpParticipationService` afhangt van de interfaces `HelpParticipationRepository` en `HelpRequestProvider`, kunnen we in productie de echte database-klasse meegeven, en in tests de snelle in-memory Fakes.

---

## 5. Integratietests met `testApplication`

In `HelpParticipationRoutesTest.kt` testen we de HTTP API zoals een echte frontend of mobiele app die aanroept:
1. **Publieke endpoints:** Verificatie dat `calendar.ics` en `participants/summary` zonder token werken.
2. **Beveiliging:** Verificatie dat alle beschermde endpoints `401 Unauthorized` teruggeven zonder token.
3. **Happy flow aanmelden & afmelden:** Aanmelden levert `201 Created` op, afmelden `204 NoContent`.
4. **Idempotentie en Conflicten:** Twee keer aanmelden levert netjes `409 Conflict` op met code `"ALREADY_PARTICIPATING"`.
5. **Autorisatie:** Alleen de eigenaar kan de check-in code opvragen; andere gebruikers krijgen `403 Forbidden` met code `"NOT_HELP_REQUEST_OWNER"`.
6. **Geofencing & Code validatie:** Juiste code + locatie geeft `200 OK`; foute code of locatie >150m geeft `400 Bad Request` met respectievelijk `"INVALID_CHECK_IN_CODE"` of `"LOCATION_TOO_FAR"`.
