# 5. Errors and the database

How the API reports problems, and how the repositories store data.

---

## Errors and validation

Every error in this API looks the same, and no route ever builds an error response by hand.

Files: `backend/src/shared/error/` and `backend/src/plugins/StatusPages.kt`.

### One error format

```json
{ "code": "USER_NOT_FOUND", "message": "User not found" }
```

- `code` is **stable** and meant for programs. The Android app can write `if (error.code == "EMAIL_ALREADY_REGISTERED")`.
  Never change an existing code.
- `message` is for people and may change.
- There are never stack traces, SQL or class names in an error. They would help attackers and confuse users.

### Throwing: `AppException`

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

### Validating input: `requireValid()`

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

### Catching: the StatusPages plugin

```kotlin
// backend/src/plugins/StatusPages.kt (shortened)
install(StatusPages) {
    exception<AppException> { call, cause ->
        call.respond(cause.toHttpStatus(), ErrorResponse(cause.code, cause.message))
    }
    exception<BadRequestException> { call, _ ->           // body missing or wrong JSON
        call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "Request body is missing or malformed"))
    }
    exception<ContentTransformationException> { call, _ -> // no or wrong Content-Type
        call.respond(HttpStatusCode.UnsupportedMediaType, ErrorResponse("UNSUPPORTED_MEDIA_TYPE", "..."))
    }
    exception<Throwable> { call, cause ->                 // a bug
        logger.error("Unhandled request error", cause)
        call.respond(HttpStatusCode.InternalServerError, ErrorResponse("INTERNAL_ERROR", "Internal server error"))
    }
    status(HttpStatusCode.Unauthorized) { call, status -> ... }   // Ktor's own 401 (bad token)
    status(HttpStatusCode.NotFound) { call, status -> ... }       // unknown URL
    status(HttpStatusCode.MethodNotAllowed) { call, status -> ... } // known URL, wrong method
    status(HttpStatusCode.TooManyRequests) { call, status -> ... } // rate limit
}
```

The plugin wraps every request. When an exception escapes a route, it picks the handler of the **most specific**
class, whatever the order: an `AppException` never reaches the `Throwable` handler.

- An `AppException` becomes its status and `{ code, message }`.
- A malformed body becomes 400.
- A body without `Content-Type: application/json` becomes 415. It is the client's mistake, so it must not be a
  500; the app sees right away that a header is missing.
- Anything else is a **bug**: it is logged with the full stack trace on the server, and the client sees only
  `INTERNAL_ERROR`.

### All status codes

Every status code and error code of the API is listed in `TECHNICAL_DESIGN.md` §5. That table is the only list;
add a new code there.

### Why this design?

- **Routes stay short.** A route reads like the happy path; errors fly out as exceptions.
- **One place to change.** Want to add a field to every error? Change `ErrorResponse` and `StatusPages`.
- **Consistent for the client.** The Android app needs one piece of code to read every error.

### Rules for your own code

- Throw an `AppException` subclass; never `call.respond(HttpStatusCode.BadRequest, ...)` in a route.
- Use `requireValid()`, never `require()`, for input checks.
- Choose a clear, stable `code` in `UPPER_SNAKE_CASE`, for example `CATEGORY_NAME_TAKEN`.
- Add the new code to the error table in `TECHNICAL_DESIGN.md` §5 and to `openapi.yaml`.

---

## Database

The tables, how a repository talks to the database, and what to do when you change a table.

Files: `backend/src/config/DatabaseFactory.kt`, every `database/` and `repository/` folder.

### The data model

The tables and their relations are drawn in `TECHNICAL_DESIGN.md` §3. Three rules to remember:

- **IDs are UUIDs** generated by the server, such as `3f2c4a1e-...`. They cannot be guessed, unlike 1, 2, 3, and the
  client never chooses them.
- **Times are UTC** (`Instant`) and are sent as ISO 8601: `2026-11-05T09:30:00Z`. The `Z` means UTC.
- **Users are never deleted**, only deactivated (`active = false`). Feedback is deleted for real.

### Connecting and creating tables

```kotlin
// backend/src/config/DatabaseFactory.kt
object DatabaseFactory {
    fun init(config: AppConfig) {
        Database.connect(url = config.databaseUrl, driver = config.databaseDriver, ...)

        transaction {
            SchemaUtils.create(UsersTable, RolesTable, PermissionsTable, UserRolesTable, RolePermissionsTable, FeedbackTable)
        }
    }
}
```

`SchemaUtils.create` creates every table that does not exist yet. It is called at startup, before the server accepts
requests.

#### Changing a table: delete the local database

`SchemaUtils.create` only **creates missing tables**. It does not add a column to a table that already exists. So:

| You did | What to do |
|---|---|
| Added a new table | Nothing: it is created at the next start |
| Added, removed or changed a column of an existing table | Stop the server, delete `data/brul*`, start again |

Deleting the file also deletes your test data; run `1-authentication.http` again afterwards. A real production system
would use a **migration tool** such as Flyway, which applies numbered SQL scripts. That is a deliberate omission to
keep this project small.

### Repositories

A repository is the only code that talks to the database. Each one has an **interface** and an **Exposed
implementation**:

```kotlin
// backend/src/features/feedback/repository/FeedbackRepository.kt
interface FeedbackRepository {
    fun findById(id: UUID): Feedback?
    fun search(query: FeedbackQuery): List<Feedback>
    fun count(query: FeedbackQuery): Long
    fun save(feedback: Feedback): Feedback
    fun updateMessage(id: UUID, message: String): Boolean
    fun updateStatus(id: UUID, status: FeedbackStatus): Boolean
    fun delete(id: UUID): Boolean
}
```

The service only knows the interface. That has two effects:

1. The service cannot accidentally run SQL or see a `ResultRow`.
2. Tests replace the database with a fake that keeps the data in a map (chapter 7).

#### One transaction per method

```kotlin
// backend/src/features/feedback/repository/ExposedFeedbackRepository.kt
override fun updateStatus(id: UUID, status: FeedbackStatus): Boolean = transaction {
    FeedbackTable.update({ FeedbackTable.id eq id }) { it[FeedbackTable.status] = status } > 0
}
```

Each repository method opens its own `transaction { }`. For this project that is enough: no operation needs several
statements to succeed or fail together.

#### Write only the column that changes

There is no `update(feedback)` that writes the whole row back. Suppose there were: an admin reads an item and closes
it, while the owner reads the same item and edits the message. The owner's request writes its copy back, with the
**old** status, and the admin's change is gone without an error. With one function per change (`updateMessage`,
`updateStatus`), each `UPDATE` touches only its own column, so both changes survive. The service still reads the
item first, for the 404.

#### From row to model: database mappers

```kotlin
// backend/src/features/feedback/mapper/FeedbackDatabaseMapper.kt
fun ResultRow.toFeedback(): Feedback =
    Feedback(
        id = this[FeedbackTable.id],
        ownerId = this[FeedbackTable.ownerId],
        message = this[FeedbackTable.message],
        status = this[FeedbackTable.status],
        createdAt = this[FeedbackTable.createdAt]
    )
```

### Constraints: let the database guard the rules

Some rules are checked twice: once in the service (for a clear error) and once by the database (as the final guard).

#### Unique values

`email` has a unique index. The service first checks `findByEmail(email) != null` and throws 409. But two
registrations with the same email could arrive at the same moment, both pass the check, and both insert. The unique
index makes the second insert fail, and the repository turns that failure into 409 too:

```kotlin
// backend/src/features/user/repository/ExposedUserRepository.kt
override fun save(user: User): User =
    try {
        transaction {
            maxAttempts = 1          // see below
            UsersTable.insert { ... }
        }
        user
    } catch (exception: ExposedSQLException) {
        if (exception.sqlState == UNIQUE_VIOLATION) {      // "23505"
            throw AppException.Conflict("Email address is already registered", "EMAIL_ALREADY_REGISTERED")
        }
        throw exception
    }
```

`23505` is the standard SQL code for "unique constraint violated".

`maxAttempts = 1`: Exposed **retries a failed transaction** three times by default. For a duplicate email a retry can
never succeed, so it would only waste time and fill the log.

#### Foreign keys

`feedback.owner_id` references `users.id`:

```kotlin
val ownerId = reference("owner_id", UsersTable.id)
```

The database refuses a feedback item for a user that does not exist.

### Atomic updates

Logging out increases the token version. Reading the value, adding 1 in Kotlin and writing it back could lose an
update if two requests run at the same time. So the increment is one SQL statement:

```kotlin
UsersTable.update({ UsersTable.id eq id }) {
    with(SqlExpressionBuilder) {
        it[tokenVersion] = tokenVersion + 1        // UPDATE users SET token_version = token_version + 1
    }
}
```

### Timestamps

```kotlin
createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
```

`Instant.now()` has nanosecond precision; the database column does not store that much. Cutting it to whole seconds
makes the stored value exactly equal to the value in memory (tests compare them) and gives clean timestamps in the API.

## Check yourself

Quiz: [errors and validation](09-quiz.md#errors-and-validation-chapter-5) and [database](09-quiz.md#database-chapter-5).
Next: [chapter 6, lists](06-lists.md).
