# 5. Life of a request

In this chapter you follow one request through the whole application. Open the files as you read; this is the most
useful chapter for understanding how the pieces fit.

The request: a user reads one of their own feedback items.

```http
GET /api/v1/feedback/3f2c...e91 HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## The overview

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

## Step by step

### 1. Routing finds the handler

`plugins/Routing.kt` registers the feedback routes under `/api/v1`, inside `authenticate(JWT_AUTHENTICATION)`. So
before any feedback code runs, Ktor first checks the token.

### 2. Authentication checks the token

`plugins/Authentication.kt`:

1. `verifier(jwtService.verifier)` checks the signature, issuer, audience and expiry date of the token.
2. `validate { ... }` then reads the user id from the `sub` claim and the token version from the `ver` claim, loads
   the user and accepts the token only if the user exists, is active and has the same token version.

If anything fails, the handler never runs and the client gets **401**:

```json
{ "code": "UNAUTHORIZED", "message": "Authentication required" }
```

### 3. The route handler runs

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

### 4. The access check

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

### 5. The service and the repository

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

### 6. The response

`call.respond(...)` hands the `FeedbackResponse` to the ContentNegotiation plugin, which turns it into JSON:

```json
{
  "id": "3f2c...e91",
  "userId": "8a1d...0b4",
  "message": "The app is slow on my phone",
  "status": "OPEN",
  "createdAt": "2026-11-05T09:30:00Z"
}
```

Finally CallLogging writes one line: `GET /api/v1/feedback/3f2c...e91 -> 200 OK`.

## When something fails

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

Chapter 8 explains this mechanism in detail.

## Try it yourself

1. Put a breakpoint in `FeedbackRoutes.kt` on `call.uuidParameter("id")`. In IntelliJ, click the green arrow next to
   `fun main()` in `Application.kt` and choose **Debug**. Send the request from `4-feedback.http` and step through it.
2. Send the same request with the token of another (non-admin) user. Where exactly does it stop?
3. Send it with an id that does not exist. Which line throws?

## Check yourself

[Quiz, chapter 5](14-quiz.md#chapter-5-life-of-a-request). Next: [chapter 6, authentication](06-authentication.md).
