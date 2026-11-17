# 2. Kotlin, Ktor and Exposed

This chapter has two parts: the Kotlin you need to read this project, and the libraries it is built on. Every
example comes from the code.

---

## Kotlin primer

Only the Kotlin you need to read this project. If you know Java, Kotlin will feel familiar; if you know Python or
JavaScript, the syntax is new but the ideas are not.

### Values and variables

```kotlin
val name = "Alice"      // val: cannot be reassigned (like final in Java). Use this by default.
var count = 0           // var: can be reassigned. This project almost never needs it.
```

Kotlin works out the type itself (`String`, `Int`). You can also write it: `val name: String = "Alice"`.

### Functions

```kotlin
fun add(a: Int, b: Int): Int {
    return a + b
}

fun add(a: Int, b: Int): Int = a + b      // the same function as a single expression
```

Parameters can have **default values** and you can pass them **by name**:

```kotlin
// backend/src/shared/paging/Page.kt
data class Page(val limit: Int = DEFAULT_LIMIT, val offset: Int = 0)

Page()                         // limit 20, offset 0
Page(limit = 5, offset = 10)   // named arguments: clear what each number means
```

### Null safety

In Kotlin a normal type can never be `null`. If something may be missing, its type ends with `?`.

```kotlin
val email: String = "a@example.com"   // never null
val user: User? = repository.findById(id)   // null when the user does not exist
```

The compiler forces you to handle the `null` case. Three operators do most of the work:

| Operator | Meaning | Example |
|---|---|---|
| `?.` | "If not null, then ..." | `singleOrNull()?.toUser()` |
| `?:` | "Otherwise use this" (the Elvis operator) | `repository.findById(id) ?: throw NotFound(...)` |
| `!!` | "I promise it is not null" (crashes if wrong) | **Not allowed in this project** |

The pattern `?: throw` appears everywhere. Read it as: *find it, or fail with a clear error*:

```kotlin
// backend/src/features/user/service/UserService.kt
private fun findUser(id: UUID): User =
    repository.findById(id) ?: throw AppException.NotFound("User not found", "USER_NOT_FOUND")
```

### Classes

#### Data classes

A `data class` holds data. Kotlin generates `equals`, `toString` and `copy` for you.

```kotlin
// backend/src/features/user/model/User.kt
data class User(
    val id: UUID,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val active: Boolean,
    val createdAt: Instant,
    val tokenVersion: Int = 0
)
```

All properties are `val`, so a `User` never changes. To "change" one, you make a **copy** with different values:

```kotlin
val updated = user.copy(displayName = "Alicia")   // everything else stays the same
```

#### Normal classes and constructor parameters

```kotlin
// backend/src/features/user/service/UserService.kt
class UserService(private val repository: UserRepository) {
    fun getById(id: UUID): UserResponse = findUser(id).toResponse()
}
```

`private val repository` in the brackets is the constructor and a private property at the same time. This is how
every service receives what it needs (see chapter 3, "Wiring").

#### Interfaces

An interface describes *what* something can do, not *how*:

```kotlin
interface UserRepository {
    fun findById(id: UUID): User?
    fun save(user: User): User
}

class ExposedUserRepository : UserRepository {          // ": UserRepository" means "implements"
    override fun findById(id: UUID): User? = ...
}
```

The service only knows the interface. The real application gives it `ExposedUserRepository` (a database);
the tests give it `FakeUserRepository` (a map in memory).

#### Objects

`object` creates exactly one instance. The project uses it for database tables and `DatabaseFactory`:

```kotlin
object UsersTable : Table("users") { ... }
```

#### Companion objects

Things that belong to the class rather than to one instance, such as constants:

```kotlin
data class Page(...) {
    companion object {
        const val DEFAULT_LIMIT = 20
    }
}

Page.DEFAULT_LIMIT    // used through the class name
```

#### Enum classes

A fixed list of values. They can have properties:

```kotlin
// backend/src/features/authorization/model/Permission.kt
enum class Permission(val key: String) {
    USER_READ("user.read"),
    USER_UPDATE("user.update"),
    ...
}

Permission.USER_READ.key     // "user.read"
Permission.entries           // all values as a list
```

#### Sealed classes

A sealed class has a fixed set of subclasses, all known to the compiler. The project uses one for errors:

```kotlin
// backend/src/shared/error/AppException.kt
sealed class AppException(val code: String, override val message: String) : RuntimeException(message) {
    class BadRequest(message: String, code: String = "INVALID_REQUEST") : AppException(code, message)
    class NotFound(message: String, code: String) : AppException(code, message)
    ...
}
```

Because the compiler knows every subclass, a `when` over them can be checked for completeness (next section).

### `when`

`when` is a better `switch`. It returns a value:

```kotlin
// backend/src/plugins/StatusPages.kt
private fun AppException.toHttpStatus(): HttpStatusCode =
    when (this) {
        is AppException.BadRequest -> HttpStatusCode.BadRequest
        is AppException.Unauthorized -> HttpStatusCode.Unauthorized
        is AppException.Forbidden -> HttpStatusCode.Forbidden
        is AppException.NotFound -> HttpStatusCode.NotFound
        is AppException.Conflict -> HttpStatusCode.Conflict
    }
```

There is no `else` branch. If someone adds a new subclass to `AppException`, this code no longer compiles until the
new case is handled. That is the benefit of `sealed`.

### Extension functions

You can add a function to an existing type without changing that type. The project uses this for every conversion:

```kotlin
// backend/src/features/user/mapper/UserMapper.kt
fun User.toResponse(): UserResponse =
    UserResponse(
        id = id.toString(),
        email = email,
        displayName = displayName,
        active = active,
        createdAt = createdAt.toString()
    )

val response = user.toResponse()    // reads like a method of User
```

Inside the function, `id`, `email` and so on refer to the `User` it was called on. Extension functions are also used
on library types, for example `ApplicationCall.currentUserId()` and `Parameters.toFeedbackQuery()`.

### Lambdas

A lambda is a function without a name, written between `{ }`:

```kotlin
val names = users.map { user -> user.displayName }
val names = users.map { it.displayName }            // "it" is the name of a single parameter
```

If the last parameter of a function is a lambda, you may write it **after** the brackets. When it is the only
parameter, the brackets disappear. This is why Ktor and Exposed code looks like a language of its own:

```kotlin
transaction { ... }                 // transaction(statement = { ... })
get("/users") { ... }               // get("/users", body = { ... })
UsersTable.selectAll().where { UsersTable.id eq id }
```

### Collections

| Function | What it does | Example |
|---|---|---|
| `map` | Transform every element | `users.map { it.toResponse() }` |
| `filter` | Keep the elements that match | `users.filter { it.active }` |
| `find` | First match, or `null` | `entries.find { it.key == key }` |
| `any` / `none` | Does any / no element match? | `items.any { it.id == id }` |
| `associateBy` | Make a map from a list | `FeedbackStatus.entries.associateBy { it.name }` |
| `drop` / `take` | Skip / keep the first n | `list.drop(offset).take(limit)` |
| `sortedBy` | Sort by a property | `users.sortedBy { it.email }` |

Lists created with `listOf(...)` cannot be changed; `mutableListOf(...)` and `mutableMapOf()` can.

### Scope functions: `let` and `also`

`let` runs a block with the value as `it`, often together with `?.` to run it only when the value is not null:

```kotlin
// backend/src/features/feedback/repository/ExposedFeedbackRepository.kt
query.status?.let { status -> rows.andWhere { FeedbackTable.status eq status } }
```

Read it as: *if a status filter was given, add a condition for it*.

`also` runs a block and then returns the original value:

```kotlin
override fun save(user: User): User = user.also { users[it.id] = it }
```

### String templates

```kotlin
"Missing permission: ${permission.key}"
"$name must be true or false"
```

### Exceptions

`throw` works like in Java. A `try` is an expression and can return a value:

```kotlin
fun verify(password: String, passwordHash: String): Boolean =
    try {
        BCrypt.checkpw(password, passwordHash)
    } catch (_: IllegalArgumentException) {
        false
    }
```

### Packages and imports

The first line of every file is its package, for example `package nl.brul.backend.features.user.service`. It
matches the folder. This project never uses wildcard imports (`import x.*`): every import names one thing, so you can
see where it comes from.

### `suspend` (just enough)

Ktor handlers run as **coroutines**: lightweight tasks that can pause while waiting for the network. You see this as
the word `suspend` on some functions, for example in the tests:

```kotlin
suspend fun ApplicationTestBuilder.register(email: String): AuthenticationResponse
```

You do not need to understand coroutines for this project. Rule of thumb: a `suspend` function can only be called
from another `suspend` function or from a Ktor handler or test block.

---

## Ktor, Exposed and friends

| Library | Job |
|---|---|
| **Ktor** | The web server: receives HTTP requests and sends responses |
| **Exposed** | Talks to the database with Kotlin code instead of SQL strings |
| **H2** | The database itself: a small SQL database stored in one file |
| **kotlinx.serialization** | Turns Kotlin objects into JSON and back |
| **Auth0 java-jwt** | Creates and checks login tokens (JWT) |
| **jBCrypt** | Hashes passwords |
| **dotenv-kotlin** | Reads the `.env` file |
| **Logback / SLF4J** | Logging |
| **JetBrains Amper** | The build tool: compiles, runs and tests the project |

All dependencies are listed in `backend/module.yaml`.

### Amper: the build tool

Most Kotlin projects use Gradle. This one uses **Amper**, which needs much less configuration. Two files matter:

```yaml
# project.yaml (repository root): which modules exist
modules:
  - backend
```

```yaml
# backend/module.yaml: what the module is and what it needs
product: jvm/app

settings:
  kotlin:
    serialization: json
  jvm:
    release: 21
    mainClass: nl.brul.backend.ApplicationKt
  ktor: enabled

dependencies:
  - $ktor.server.core
  - org.jetbrains.exposed:exposed-core:0.61.0
  ...

test-dependencies:
  - $ktor.server.testHost
```

- `$ktor.server.core` is a shortcut that Amper provides when `ktor: enabled`; it picks a matching version.
- Other libraries are written as `group:name:version`.
- Amper expects sources in `backend/src/`, resources in `backend/resources/` and tests in `backend/test/`. There is
  no `src/main/kotlin` like in Gradle.

> Adding a dependency is a decision, not a detail: every library is something you must be able to explain. Ask
> first.

### Ktor: the web server

#### The application and plugins

A Ktor application is a set of **plugins** (features you switch on) and **routes** (which code answers which URL).

```kotlin
// backend/src/Application.kt
fun main() {
    val config = AppConfig.load()
    DatabaseFactory.init(config)
    embeddedServer(factory = Netty, port = config.port, host = config.host, module = { module(config) })
        .start(wait = true)
}
```

`embeddedServer` starts a Netty web server; `module(config)` sets everything up. Every plugin is installed in its own
file in `backend/src/plugins/`:

| File | Plugin | Job |
|---|---|---|
| `Serialization.kt` | ContentNegotiation | Converts request and response bodies to and from JSON |
| `StatusPages.kt` | StatusPages | Turns exceptions into error responses |
| `Authentication.kt` | Authentication (JWT) | Checks the login token |
| `RateLimiting.kt` | RateLimit | Limits login attempts |
| `CallLogging.kt` | CallLogging | Logs one line per request |
| `Routing.kt` | Routing | Connects URLs to feature routes |

#### Routing

Routes are written as nested blocks:

```kotlin
// backend/src/plugins/Routing.kt (shortened)
routing {
    route("/api/v1") {
        authenticate(JWT_AUTHENTICATION) {      // everything inside needs a valid token
            userRoutes(userService, authorizationService)
        }
    }
}
```

```kotlin
// backend/src/features/user/route/UserRoutes.kt (shortened)
fun Route.userRoutes(userService: UserService, authorizationService: AuthorizationService) {
    route("/users") {
        get("/me") {                                        // GET /api/v1/users/me
            call.respond(userService.getById(call.currentUserId()))
        }
        patch("/{id}") {                                    // PATCH /api/v1/users/{id}
            val id = call.uuidParameter("id")
            ...
        }
    }
}
```

#### The call

Inside a route handler, `call` represents the current request and its response:

| Code | Meaning |
|---|---|
| `call.receive<UpdateUserRequest>()` | Read the JSON body as this class (400 when it does not fit) |
| `call.respond(response)` | Send an object as JSON with status 200 |
| `call.respond(HttpStatusCode.Created, response)` | Send it with another status |
| `call.respond(HttpStatusCode.NoContent)` | Send 204 without a body |
| `call.parameters["id"]` | A path parameter such as `{id}` |
| `call.request.queryParameters` | The query string, such as `?q=alice` |

The project adds helpers on top: `call.uuidParameter("id")` and `call.currentUserId()`.

### kotlinx.serialization: JSON

A class with `@Serializable` can be converted to and from JSON. The JSON field names are the property names.

```kotlin
@Serializable
data class UpdateUserRequest(val displayName: String)
```

```json
{ "displayName": "Alicia" }
```

If the JSON misses a field or has the wrong type, `call.receive` fails and the API answers 400. Only DTOs (the
classes in `dto/`) are serializable; domain models are not, so a model can never be sent to the client by accident.

### Exposed: the database

Exposed lets you describe tables and queries in Kotlin. This project uses the **DSL** style: tables as objects,
queries as functions. (Exposed also has a "DAO" style with entity classes; it is not used here.)

#### A table

```kotlin
// backend/src/features/user/database/UsersTable.kt
object UsersTable : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 320).uniqueIndex()
    val displayName = varchar("display_name", 120)
    val passwordHash = varchar("password_hash", 255)
    val active = bool("active").default(true)
    val createdAt = timestamp("created_at")
    val tokenVersion = integer("token_version").default(0)

    override val primaryKey = PrimaryKey(id)
}
```

Kotlin names are `camelCase`, the database names `snake_case`.

#### Transactions

Every database operation runs inside `transaction { }`. Everything in the block succeeds together or fails together.

#### Reading

```kotlin
UsersTable.selectAll().where { UsersTable.id eq id }    // SELECT * FROM users WHERE id = ?
    .singleOrNull()                                     // one row or null
    ?.toUser()                                          // ResultRow -> User (a mapper)
```

`eq` means `=`. Others: `neq`, `less`, `greater`, `like`, `and`, `or`.

#### Writing

```kotlin
UsersTable.insert {
    it[id] = user.id
    it[email] = user.email
}

UsersTable.update({ UsersTable.id eq id }) {
    it[UsersTable.displayName] = displayName      // only the column that changes
}

FeedbackTable.deleteWhere { FeedbackTable.id eq id }
```

`update` and `deleteWhere` return the number of rows they changed. The repositories turn that into `true`/`false`.

#### ResultRow

A query returns rows of type `ResultRow`. You read a column with `row[UsersTable.email]`. The mappers in
`mapper/*DatabaseMapper.kt` turn a `ResultRow` into a domain model, so no other layer ever sees a `ResultRow`.

### H2: the database

H2 is a SQL database written in Java. With `DATABASE_URL=jdbc:h2:file:./data/brul` it stores everything in
`data/brul.mv.db`. The tests use `jdbc:h2:mem:...`: a database that only lives in memory during the test.

### JWT, BCrypt and logging

These get their own explanation in chapter 4. Logging needs just one rule: **never log passwords,
tokens, secrets or the `Authorization` header**. `CallLogging.kt` logs only the method, the path and the status.

## Check yourself

Quiz: [Kotlin](09-quiz.md#kotlin-chapter-2) and [libraries](09-quiz.md#libraries-chapter-2). Next: [chapter 3,
architecture and the life of a request](03-architecture-and-requests.md).
