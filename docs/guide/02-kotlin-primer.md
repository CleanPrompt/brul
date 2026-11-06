# 2. Kotlin primer

This chapter covers only the Kotlin you need to read this project. Each feature comes with an example from the code.
If you know Java, Kotlin will feel familiar; if you know Python or JavaScript, the syntax is new but the ideas are not.

## Values and variables

```kotlin
val name = "Alice"      // val: cannot be reassigned (like final in Java). Use this by default.
var count = 0           // var: can be reassigned. This project almost never needs it.
```

Kotlin works out the type itself (`String`, `Int`). You can also write it: `val name: String = "Alice"`.

## Functions

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

## Null safety

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

## Classes

### Data classes

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

### Normal classes and constructor parameters

```kotlin
// backend/src/features/user/service/UserService.kt
class UserService(private val repository: UserRepository) {
    fun getById(id: UUID): UserResponse = findUser(id).toResponse()
}
```

`private val repository` in the brackets is the constructor and a private property at the same time. This is how
every service receives what it needs (see chapter 4, "dependency injection by hand").

### Interfaces

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

### Objects

`object` creates exactly one instance. The project uses it for database tables and `DatabaseFactory`:

```kotlin
object UsersTable : Table("users") { ... }
```

### Companion objects

Things that belong to the class rather than to one instance, such as constants:

```kotlin
data class Page(...) {
    companion object {
        const val DEFAULT_LIMIT = 20
    }
}

Page.DEFAULT_LIMIT    // used through the class name
```

### Enum classes

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

### Sealed classes

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

## `when`

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

## Extension functions

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

## Lambdas

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

## Collections

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

## Scope functions: `let` and `also`

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

## String templates

```kotlin
"Missing permission: ${permission.key}"
"$name must be true or false"
```

## Exceptions

`throw` works like in Java. A `try` is an expression and can return a value:

```kotlin
fun verify(password: String, passwordHash: String): Boolean =
    try {
        BCrypt.checkpw(password, passwordHash)
    } catch (_: IllegalArgumentException) {
        false
    }
```

## Packages and imports

The first line of every file is its package, for example `package nl.brul.backend.features.user.service`. It
matches the folder. This project never uses wildcard imports (`import x.*`): every import names one thing, so you can
see where it comes from.

## `suspend` (just enough)

Ktor handlers run as **coroutines**: lightweight tasks that can pause while waiting for the network. You see this as
the word `suspend` on some functions, for example in the tests:

```kotlin
suspend fun ApplicationTestBuilder.register(email: String): AuthenticationResponse
```

You do not need to understand coroutines for this project. Rule of thumb: a `suspend` function can only be called
from another `suspend` function or from a Ktor handler or test block.

## Check yourself

Go to the [quiz](14-quiz.md#chapter-2-kotlin) and answer the Kotlin questions. Next:
[chapter 3, the libraries](03-ktor-and-exposed-primer.md).
