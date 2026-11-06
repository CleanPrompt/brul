# 3. Ktor, Exposed and friends

This chapter introduces the libraries. You will see each of them in action in later chapters.

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

## Amper: the build tool

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

## Ktor: the web server

### The application and plugins

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

### Routing

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

### The call

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

## kotlinx.serialization: JSON

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

## Exposed: the database

Exposed lets you describe tables and queries in Kotlin. This project uses the **DSL** style: tables as objects,
queries as functions. (Exposed also has a "DAO" style with entity classes; it is not used here.)

### A table

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

### Transactions

Every database operation runs inside `transaction { }`. Everything in the block succeeds together or fails together.

### Reading

```kotlin
UsersTable.selectAll().where { UsersTable.id eq id }    // SELECT * FROM users WHERE id = ?
    .singleOrNull()                                     // one row or null
    ?.toUser()                                          // ResultRow -> User (a mapper)
```

`eq` means `=`. Others: `neq`, `less`, `greater`, `like`, `and`, `or`.

### Writing

```kotlin
UsersTable.insert {
    it[id] = user.id
    it[email] = user.email
}

UsersTable.update({ UsersTable.id eq user.id }) {
    it[displayName] = user.displayName
}

FeedbackTable.deleteWhere { FeedbackTable.id eq id }
```

`update` and `deleteWhere` return the number of rows they changed. The repositories turn that into `true`/`false`.

### ResultRow

A query returns rows of type `ResultRow`. You read a column with `row[UsersTable.email]`. The mappers in
`mapper/*DatabaseMapper.kt` turn a `ResultRow` into a domain model, so no other layer ever sees a `ResultRow`.

## H2: the database

H2 is a SQL database written in Java. With `DATABASE_URL=jdbc:h2:file:./data/brul` it stores everything in
`data/brul.mv.db`. The tests use `jdbc:h2:mem:...`: a database that only lives in memory during the test.

## JWT, BCrypt and logging

These get their own explanation in chapter 6 (JWT and BCrypt). Logging needs just one rule: **never log passwords,
tokens, secrets or the `Authorization` header**. `CallLogging.kt` logs only the method, the path and the status.

## Check yourself

[Quiz, chapter 3](14-quiz.md#chapter-3-libraries). Next: [chapter 4, architecture](04-architecture.md).
