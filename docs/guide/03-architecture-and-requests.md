# 3. Architecture and the life of a request

First how the code is organised and why; then one request followed through the whole application. Once you understand
this chapter, you can find anything in the project.

---

## Architecture

### A feature-first modular monolith

That is a long name for two simple ideas:

- **Monolith**: one application, one deployable program, one database. No microservices.
- **Feature-first (modular)**: the code is grouped by *what it is about* (users, feedback, roles), not by *what kind of
  code it is* (all controllers together, all services together).

```text
backend/src/
├── Application.kt          entry point: real repositories, then configureApp (plugins and routes)
├── AppServices.kt          builds every service once; shared by the app and the tests
├── config/                 AppConfig (settings), DatabaseFactory (connection, tables)
├── plugins/                Ktor plugin setup only; no business logic
├── shared/                 code that every feature uses
│   ├── error/              AppException, ErrorResponse
│   ├── http/               reading path and query parameters
│   ├── paging/             Page, SortDirection, PageResponse
│   └── database/           Exposed helpers for search and sorting
└── features/
    ├── authentication/     who is the caller? register, login, tokens
    ├── authorization/      what may the caller do? roles, permissions
    ├── user/               user accounts
    ├── feedback/           the reference feature: copy this one
    └── health/             GET /health
```

Why by feature? When you work on feedback, everything you need is in `features/feedback/`. When you delete a feature,
you delete one folder (and its wiring). Features stay small and independent.

### The layers inside a feature

Every feature uses the same layers, always in the same folders:

```mermaid
flowchart TD
    Client([HTTP client]) --> Route
    Route["route/<br/>HTTP only: read the request, check access,<br/>call the service, send the response"] --> Service
    Service["service/<br/>business rules and validation"] --> Repository
    Repository["repository/<br/>interface + Exposed implementation"] --> Database
    Database["database/<br/>table definitions"] --> H2[(H2)]
    Mapper["mapper/<br/>all conversions"] -.used by.-> Route
    Mapper -.used by.-> Service
    Mapper -.used by.-> Repository
```

| Folder | Contains | Rule |
|---|---|---|
| `route/` | `FeedbackRoutes.kt` | Only HTTP. No business rules, no database code |
| `service/` | `FeedbackService.kt` | Business rules. Never sees `ApplicationCall` or `ResultRow` |
| `repository/` | `FeedbackRepository` (interface) and `ExposedFeedbackRepository` | Only database access. Returns domain models |
| `database/` | `FeedbackTable` | Exposed table definitions |
| `model/` | `Feedback`, `FeedbackStatus`, `FeedbackQuery` | Domain objects: what the application works with |
| `dto/` | `FeedbackRequest`, `FeedbackResponse` | Data Transfer Objects: the JSON the client sends and receives |
| `mapper/` | `toResponse()`, `toFeedback()`, `toFeedbackQuery()` | Every conversion between representations |

A feature only has the folders it needs. `health/` has just `route/`.

#### Why so many layers?

Each layer has **one reason to change** (the Single Responsibility Principle):

- The JSON contract changes? Only `dto/` and `mapper/`.
- A business rule changes ("a message may be 5000 characters")? Only `service/`.
- We switch from H2 to PostgreSQL? Only `repository/` and `database/`.

It also makes testing easy: the service can be tested with a fake repository, without a database (chapter 7).

#### Three representations of the same thing

A feedback item exists in three shapes, and mappers convert between them:

```mermaid
flowchart LR
    Row["ResultRow<br/>(database row)"] -- "toFeedback()" --> Model["Feedback<br/>(domain model)"]
    Model -- "toResponse()" --> Dto["FeedbackResponse<br/>(JSON for the client)"]
```

Why not send the model directly? Look at `User`: it has `passwordHash` and `tokenVersion`. `UserResponse` leaves them
out. Because only DTOs are `@Serializable`, a password hash can never end up in a response by accident.

### Wiring: `AppServices` and `Application.kt`

Somewhere, every object has to be created and handed what it needs. Many frameworks do this automatically (Spring,
Koin). This project does it **by hand**, in three small pieces:

```kotlin
// backend/src/AppServices.kt (shortened): builds every service once
class AppServices(
    val userRepository: UserRepository,
    val authorizationRepository: AuthorizationRepository,
    val feedbackRepository: FeedbackRepository,
    val profileImageRepository: ProfileImageRepository,
    config: AppConfig
) {
    val jwtService = JwtService(config)
    val authorizationService = AuthorizationService(authorizationRepository, userRepository)
    val userService = UserService(userRepository)
    val roleService = RoleService(authorizationRepository, userService)
    val feedbackService = FeedbackService(feedbackRepository)
    ...
}
```

```kotlin
// backend/src/Application.kt (shortened)
fun Application.module(config: AppConfig) {
    val authorizationRepository = ExposedAuthorizationRepository()
    AuthorizationSeeder(authorizationRepository).seed()

    val services = AppServices(
        userRepository = ExposedUserRepository(),     // the real repositories, backed by the database
        authorizationRepository = authorizationRepository,
        ...
        config = config
    )
    configureApp(services)
}

fun Application.configureApp(services: AppServices) {
    configureCallLogging()
    configureSerialization()
    configureStatusPages()
    configureRateLimiting()
    configureAuthentication(services.jwtService, services.userRepository)
    configureRouting(services)
}
```

This is called **constructor injection**: a class gets its dependencies through its constructor
(`class UserService(private val repository: UserRepository)`) instead of creating them itself. Benefits:

- You can read the whole object graph in one short class.
- Tests pass fakes into the same `AppServices` (`testServices()` in `backend/test/TestFixtures.kt`) and call the
  same `configureApp`, so they test exactly the wiring of the real application.
- No framework magic to explain.

### Shared code

`shared/` contains code that more than one feature needs. It must not depend on any feature. If something is used by
one feature only, it belongs in that feature.

### How the features depend on each other

```mermaid
flowchart LR
    authentication --> user
    authentication --> authorization
    authorization --> user
    feedback --> authorization
    feedback -. foreign key .-> user
```

- `authentication` uses `UserRepository` to find and store users, and `RoleService` to give a new user a role.
- `authorization` uses `UserRepository` to check that a user is active.
- `feedback` uses `AuthorizationService` for its access checks; its table points to `users`.

---

## Life of a request

Now follow one request through the whole application. Open the files as you read; this is the most useful part for
understanding how the pieces fit.

The request: a user reads one of their own feedback items.

```http
GET /api/v1/feedback/3f2c...e91 HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### The overview

```mermaid
sequenceDiagram
    participant C as Client
    participant K as Ktor (plugins)
    participant R as FeedbackRoutes
    participant A as AuthorizationService
    participant S as FeedbackService
    participant DB as ExposedFeedbackRepository / H2

    C->>K: GET /api/v1/feedback/{id} + token
    K->>K: Authentication: check token, user active, same version
    K->>R: route matches, call.currentUserId() = sub
    R->>S: getOwnerId(id)
    S->>DB: findById(id)
    DB-->>S: Feedback (or null -> 404)
    S-->>R: ownerId
    R->>A: requireOwnerOrPermission(currentUserId, ownerId, FEEDBACK_READ)
    A-->>R: ok (or 403)
    R->>S: getById(id)
    S-->>R: FeedbackResponse
    R-->>C: 200 OK + JSON
```

### Step by step

#### 1. Routing finds the handler

`plugins/Routing.kt` registers the feedback routes under `/api/v1`, inside `authenticate(JWT_AUTHENTICATION)`. So
before any feedback code runs, Ktor first checks the token.

#### 2. Authentication checks the token

`plugins/Authentication.kt`:

1. `verifier(jwtService.verifier)` checks the signature, issuer, audience and expiry date of the token.
2. `validate { ... }` then reads the user id from the `sub` claim and the token version from the `ver` claim, loads
   the user and accepts the token only if the user exists, is active and has the same token version.

If anything fails, the handler never runs and the client gets **401**:

```json
{ "code": "UNAUTHORIZED", "message": "Authentication required" }
```

#### 3. The route handler runs

```kotlin
// backend/src/features/feedback/route/FeedbackRoutes.kt
get("/{id}") {
    val id = call.uuidParameter("id")
    authorizationService.requireOwnerOrPermission(
        call.currentUserId(), feedbackService.getOwnerId(id), Permission.FEEDBACK_READ
    )
    call.respond(feedbackService.getById(id))
}
```

- `call.uuidParameter("id")` reads `{id}` from the URL. `not-a-uuid` gives **400**.
- `call.currentUserId()` reads the `sub` claim of the accepted token.
- `feedbackService.getOwnerId(id)` looks the item up. An unknown id gives **404** `FEEDBACK_NOT_FOUND`.

#### 4. The access check

```kotlin
// backend/src/features/authorization/service/AuthorizationService.kt
fun requireOwnerOrPermission(currentUserId: UUID, ownerId: UUID, permission: Permission) {
    if (currentUserId != ownerId) {
        requirePermission(currentUserId, permission)
    }
}
```

The owner may always read their own item. Anyone else needs `feedback.read`, which is checked against the database.
Without it: **403**.

#### 5. The service and the repository

```kotlin
// FeedbackService
fun getById(id: UUID): FeedbackResponse = findFeedback(id).toResponse()

private fun findFeedback(id: UUID): Feedback =
    repository.findById(id) ?: throw feedbackNotFound()
```

```kotlin
// ExposedFeedbackRepository
override fun findById(id: UUID): Feedback? = transaction {
    FeedbackTable.selectAll().where { FeedbackTable.id eq id }
        .singleOrNull()?.toFeedback()
}
```

The repository runs `SELECT * FROM feedback WHERE id = ?`, turns the row into a `Feedback` (mapper) and returns it.
The service turns it into a `FeedbackResponse` (mapper).

#### 6. The response

`call.respond(...)` hands the `FeedbackResponse` to the ContentNegotiation plugin, which turns it into JSON:

```json
{
  "id": "3f2c...e91",
  "ownerId": "8a1d...0b4",
  "message": "The app is slow on my phone",
  "status": "OPEN",
  "createdAt": "2026-11-05T09:30:00Z"
}
```

Finally CallLogging writes one line: `GET /api/v1/feedback/3f2c...e91 -> 200 OK`.

### When something fails

No route builds an error response itself. Every failure is an exception, and one plugin turns it into a response:

```mermaid
flowchart LR
    A["throw AppException.NotFound(...)"] --> B[StatusPages plugin]
    B --> C["404 { code, message }"]
```

| Where it fails | What is thrown | Response |
|---|---|---|
| No or invalid token | (Ktor answers 401 itself) | 401 `UNAUTHORIZED` |
| `uuidParameter` | `AppException.BadRequest` | 400 `INVALID_REQUEST` |
| `findFeedback` | `AppException.NotFound` | 404 `FEEDBACK_NOT_FOUND` |
| `requirePermission` | `AppException.Forbidden` | 403 `FORBIDDEN` |
| A bug, e.g. a `NullPointerException` | anything else | 500 `INTERNAL_ERROR`, details only in the log |

Chapter 5 explains this mechanism in detail.

### Try it yourself

1. Put a breakpoint in `FeedbackRoutes.kt` on `call.uuidParameter("id")`. In IntelliJ, click the green arrow next to
   `fun main()` in `Application.kt` and choose **Debug**. Send the request from `4-feedback.http` and step through it.
2. Send the same request with the token of another (non-admin) user. Where exactly does it stop?
3. Send it with an id that does not exist. Which line throws?

## Check yourself

Quiz: [architecture](09-quiz.md#architecture-chapter-3) and [life of a request](09-quiz.md#life-of-a-request-chapter-3).
Next: [chapter 4, security](04-security.md).
