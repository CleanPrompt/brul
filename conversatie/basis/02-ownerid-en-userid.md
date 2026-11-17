# `ownerId` en `userId`: wat is het verschil?

Uitleg over hoe het project bepaalt of je bij een ding (een feedbackitem, een account) mag, en waarom daar drie
verschillende namen voor bestaan.

---

## 1. Het korte antwoord

**Het zijn alle drie gewoon user-id's.** Ze komen allemaal uit de tabel `users`. Alleen hun **rol** in de code
verschilt.

| Naam | Waar | Betekenis |
|---|---|---|
| `currentUserId` | `call.currentUserId()` in de routes | **Wie doet het verzoek?** Komt uit het JWT-token (`sub`) |
| `userId` | `Feedback.userId`, kolom `feedback.user_id` | **Van wie is dit feedbackitem?** Opgeslagen in de database, als foreign key naar `users.id` |
| `ownerId` | parameter van `requireOwnerOrPermission(...)` | **Van wie is het ding waar het verzoek over gaat?** Een algemene naam voor de toegangscheck |

`userId` en `ownerId` betekenen bij feedback dus hetzelfde:

- De kolom heet `user_id`, omdat het een verwijzing naar de tabel `users` is.
- De check noemt het `ownerId`, omdat de check niet weet of het om feedback, een afbeelding of iets anders gaat.
  Hij weet alleen: *dit is de eigenaar*.

---

## 2. Hoe het samenwerkt

In `backend/src/features/feedback/route/FeedbackRoutes.kt`:

```kotlin
get("/{id}") {
    val id = call.uuidParameter("id")                  // welk item?
    authorizationService.requireOwnerOrPermission(
        call.currentUserId(),                          // wie vraagt het?
        feedbackService.getOwnerId(id),                // van wie is het? (= feedback.userId)
        Permission.FEEDBACK_READ                       // wat heb je anders nodig?
    )
    call.respond(feedbackService.getById(id))
}
```

De check zelf, in `backend/src/features/authorization/service/AuthorizationService.kt`:

```kotlin
fun requireOwnerOrPermission(currentUserId: UUID, ownerId: UUID, permission: Permission) {
    if (currentUserId != ownerId) {                    // ben je niet de eigenaar?
        requirePermission(currentUserId, permission)   // dan moet je de permissie hebben
    }
}
```

Dat zijn drie regels: **ben je de eigenaar, dan mag je erbij. Zo niet, dan heb je de permissie nodig.**

### Bij een gebruikersaccount

Bij `/users/{id}` is het account zelf het "ding". Daar is `ownerId` gewoon het `id` uit de URL: jij bent de
eigenaar van je eigen account.

---

## 3. Waarom bestaat het?

Zonder deze check kan iedereen die ingelogd is elk feedbackitem opvragen, als hij het id weet of raadt. Dat is
het bekendste beveiligingslek in API's. OWASP noemt het **BOLA** (*Broken Object Level Authorization*, vroeger
IDOR). Het staat op nummer 1 van de OWASP API Security Top 10.

Permissies alleen lossen dit niet op. `feedback.read` zegt *"je mag feedback lezen"*, maar niet *welke*. Je
hebt twee soorten regels nodig:

| Vraag | Antwoord in het project |
|---|---|
| **Wat** mag je doen? | De permissie (`Permission.FEEDBACK_READ`) |
| **Met welk ding**? | Het eigenaarschap (`ownerId`) |

> **Eén zin voor het assessment:** permissies zeggen *wát* je mag, eigenaarschap zegt met *wélk* ding;
> `requireOwnerOrPermission` combineert die twee.

---

## 4. Is het een goede aanpak?

**Ja, in de kern.** Eigenaarschap controleren is de standaardoplossing, en één functie die het regelt is goed
uit te leggen.

Wel drie kanttekeningen:

1. **De naam `userId` in feedback is vaag.** `userId` kan ook "de gebruiker die het verzoek doet" betekenen.
   Als de kolom en het veld `ownerId` heten, is in één oogopslag duidelijk wat het is. Dit is een kleine
   verbetering. Let op: de tabel `feedback` verandert dan, dus `data/brul*` moet weg.
2. **Er staan twee lookups achter elkaar.** `getOwnerId(id)` leest het item, daarna leest `getById(id)`
   hetzelfde item nog een keer. Dat werkt, maar het is dubbel werk en het ziet er voor een beginner vreemd uit.
3. **De volgorde verschilt per feature.**
   - Bij `/users/{id}` komt eerst de check en daarna het opzoeken. Wie geen toegang heeft krijgt 403, ook als
     het id niet bestaat.
   - Bij `/feedback/{id}` komt eerst het opzoeken en dan de check. Een onbekend id geeft 404.

   Beide keuzes zijn beargumenteerd, maar een regel met een uitzondering verwart beginners.

---

## 5. Alternatieven

### A. `/me`-routes: het id komt alleen uit het token

```text
GET /api/v1/users/me/feedback          ← altijd jouw eigen items
GET /api/v1/feedback/{id}              ← alleen voor admins (requirePermission)
```

Je kunt dan niet eens een id van iemand anders meesturen. Gewone gebruikers krijgen alleen `/me`-routes, admins
de algemene routes met `requirePermission`.

- **Voordeel:** het simpelste te begrijpen, en je hebt overal maar één soort check.
- **Nadeel:** dubbele routes voor dezelfde data.

### B. Eigenaar in de query zelf (scoped query)

```kotlin
// repository
fun findByIdAndOwner(id: UUID, ownerId: UUID): Feedback?
// SQL: WHERE id = ? AND user_id = ?
```

Is het niet van jou, dan vindt de query niets en krijg je 404.

- **Voordeel:** één query in plaats van twee, en je kunt de check niet vergeten.
- **Nadeel:** admins hebben een aparte weg nodig, en de toegangsregel zit dan in de repository. Dat botst met
  de projectregel "repository: geen permissies".

### C. Wat het project nu doet: eigenaar ophalen, dan checken

- **Voordeel:** één algemene functie en één route per actie, inclusief de override voor admins.
- **Nadeel:** twee lookups en een subtiele volgorde.

### D. Zware oplossingen

Policy-objecten, ABAC (regels op basis van eigenschappen) of row-level security in de database. Te veel voor
dit project.

---

## 6. Advies

Houd **C**. Het werkt, het is getest en het is goed te verdedigen. Maak het wel beginnersvriendelijker:

1. **Overal dezelfde volgorde:** eerst de eigenaar ophalen, dan checken.
2. **Overweeg de naam `ownerId`** voor de eigenaarskolom in nieuwe features, zoals `images`. Feedback kun je
   laten zoals het is, of gelijktrekken.
3. **Leer de zin uit hoofdstuk 3** voor het assessment.

---

## 7. Doorgevoerd (fase 14)

Advies 1 en 2 zijn doorgevoerd:

- **Eén volgorde:** ook `GET`/`PATCH /users/{id}` en `GET /users/{id}/roles` lezen nu eerst de eigenaar via
  `userService.getOwnerId(id)`. Een onbekend id geeft daar nu 404 in plaats van 403, net als bij feedback.
- **`ownerId` in feedback:** de kolom heet `owner_id`, het veld `ownerId`, ook in de JSON en als filter
  (`GET /feedback?ownerId=...`). In de code over feedback betekent `ownerId` dus altijd "de eigenaar" en
  `currentUserId` altijd "wie het verzoek doet".

Details en de reden staan in `docs/implementation-plan.md` (beslissingen "Owned items" en "Owner naming", fase 14).
