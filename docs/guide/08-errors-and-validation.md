# 8. Errors and validation

Every error in this API looks the same, and no route ever builds an error response by hand. This chapter shows how.

Files: `backend/src/shared/error/` and `backend/src/plugins/StatusPages.kt`.

## One error format

```json
{ "code": "USER_NOT_FOUND", "message": "User not found" }
```

- `code` is **stable** and meant for programs. The Android app can write `if (error.code == "EMAIL_ALREADY_REGISTERED")`.
  Never change an existing code.
- `message` is for people and may change.
- There are never stack traces, SQL or class names in an error. They would help attackers and confuse users.

## Throwing: `AppException`

Services and routes report an expected problem by throwing a subclass of `AppException`:

```kotlin
// backend/src/shared/error/AppException.kt
sealed class AppException(val code: String, override val message: String) : RuntimeException(message) {
    class BadRequest(message: String, code: String = "INVALID_REQUEST") : AppException(code, message)   // 400
    class Unauthorized(message: String, code: String = "UNAUTHORIZED") : AppException(code, message)    // 401
    class Forbidden(message: String, code: String = "FORBIDDEN") : AppException(code, message)          // 403
    class NotFound(message: String, code: String) : AppException(code, message)                         // 404
    class Conflict(message: String, code: String) : AppException(code, message)                         // 409
}
```

```kotlin
throw AppException.NotFound("Feedback not found", "FEEDBACK_NOT_FOUND")
throw AppException.Conflict("Email address is already registered", "EMAIL_ALREADY_REGISTERED")
```

`NotFound` and `Conflict` have no default code, so you are forced to choose a specific one.

## Validating input: `requireValid()`

Kotlin has `require(condition) { message }`, but it throws `IllegalArgumentException`, which would become a 500. The
project has its own version that throws a 400:

```kotlin
inline fun requireValid(condition: Boolean, message: () -> String) {
    if (!condition) {
        throw AppException.BadRequest(message())
    }
}
```

```kotlin
// backend/src/features/feedback/service/FeedbackService.kt
private fun validMessage(message: String): String {
    val trimmed = message.trim()
    requireValid(trimmed.isNotEmpty() && trimmed.length <= MAX_MESSAGE_LENGTH) {
        "Message must contain between 1 and $MAX_MESSAGE_LENGTH characters"
    }
    return trimmed
}
```

Validation lives in the **service**, because it is a business rule. The service also normalises input (trim spaces,
lowercase emails) before checking it.

## Catching: the StatusPages plugin

```kotlin
// backend/src/plugins/StatusPages.kt (shortened)
install(StatusPages) {
    exception<AppException> { call, cause ->
        call.respond(cause.toHttpStatus(), ErrorResponse(cause.code, cause.message))
    }
    exception<BadRequestException> { call, _ ->           // body missing or wrong JSON
        call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "Request body is missing or malformed"))
    }
    exception<Throwable> { call, cause ->                 // a bug
        logger.error("Unhandled request error", cause)
        call.respond(HttpStatusCode.InternalServerError, ErrorResponse("INTERNAL_ERROR", "Internal server error"))
    }
    status(HttpStatusCode.Unauthorized) { call, status -> ... }   // Ktor's own 401 (bad token)
    status(HttpStatusCode.NotFound) { call, status -> ... }       // unknown URL
    status(HttpStatusCode.TooManyRequests) { call, status -> ... } // rate limit
}
```

The plugin wraps every request. When an exception escapes a route, it picks the first matching handler:

- An `AppException` becomes its status and `{ code, message }`.
- A malformed body becomes 400.
- Anything else is a **bug**: it is logged with the full stack trace on the server, and the client sees only
  `INTERNAL_ERROR`.

## All status codes at a glance

| Status | When | Typical code |
|---|---|---|
| 400 | Invalid input, malformed JSON, invalid UUID or query parameter | `INVALID_REQUEST`, `INVALID_CURRENT_PASSWORD` |
| 401 | No or invalid token, wrong login | `UNAUTHORIZED`, `INVALID_CREDENTIALS` |
| 403 | Logged in, but not allowed | `FORBIDDEN` |
| 404 | Resource or URL does not exist | `USER_NOT_FOUND`, `FEEDBACK_NOT_FOUND`, `NOT_FOUND` |
| 409 | Conflicts with existing data | `EMAIL_ALREADY_REGISTERED` |
| 429 | Rate limit exceeded | `TOO_MANY_REQUESTS` |
| 500 | A bug | `INTERNAL_ERROR` |

## Why this design?

- **Routes stay short.** A route reads like the happy path; errors fly out as exceptions.
- **One place to change.** Want to add a field to every error? Change `ErrorResponse` and `StatusPages`.
- **Consistent for the client.** The Android app needs one piece of code to read every error.

## Rules for your own code

- Throw an `AppException` subclass; never `call.respond(HttpStatusCode.BadRequest, ...)` in a route.
- Use `requireValid()`, never `require()`, for input checks.
- Choose a clear, stable `code` in `UPPER_SNAKE_CASE`, for example `CATEGORY_NAME_TAKEN`.
- Add the new code to the error table in `docs/implementation-plan.md` and to `openapi.yaml`.

## Check yourself

[Quiz, chapter 8](14-quiz.md#chapter-8-errors-and-validation). Next: [chapter 9, the database](09-database.md).
