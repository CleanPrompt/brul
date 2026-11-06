# 15. Glossary

| Term | Meaning |
|---|---|
| **401 Unauthorized** | The server does not know who you are: no token, an invalid or expired token |
| **403 Forbidden** | The server knows who you are, but you may not do this |
| **409 Conflict** | The request conflicts with existing data, such as a duplicate email |
| **Access check** | A call such as `requirePermission(...)` at the start of a route |
| **Amper** | JetBrains' build tool used instead of Gradle; configured in `project.yaml` and `module.yaml` |
| **Audience (`aud`)** | JWT claim: who the token is meant for |
| **Authentication** | Finding out *who* the caller is (login, tokens) |
| **Authorization** | Deciding *what* the caller may do (roles, permissions) |
| **BCrypt** | A slow, salted password-hashing algorithm |
| **Bootstrap admin** | The account that registers with `BOOTSTRAP_ADMIN_EMAIL` and becomes the first admin |
| **Breaking change** | A change to the API that makes existing clients fail |
| **Claim** | One field in a JWT payload, such as `sub` or `exp` |
| **Constructor injection** | Giving a class its dependencies through its constructor |
| **Coroutine** | A lightweight task that can pause while waiting; Ktor handlers run as coroutines (`suspend`) |
| **Data class** | A Kotlin class for holding data, with generated `equals`, `toString` and `copy` |
| **Default role** | A role defined in `enum class DefaultRole` (ADMIN, USER) and created at startup |
| **Deny by default** | Everything is forbidden unless a rule allows it |
| **DTO** | Data Transfer Object: the shape of JSON sent or received (`...Request`, `...Response`) |
| **Domain model** | The objects the application works with internally (`User`, `Feedback`) |
| **Elvis operator (`?:`)** | "Use the left value, or the right one if the left is null" |
| **Enum class** | A type with a fixed list of values (`Permission`, `FeedbackStatus`) |
| **Exposed** | JetBrains' Kotlin library for SQL databases; this project uses its DSL |
| **Extension function** | A function added to an existing type from outside, such as `User.toResponse()` |
| **Fake** | A simple in-memory implementation of an interface, used in tests |
| **Feature-first** | Organising code by subject (users, feedback) instead of by technical layer |
| **Foreign key** | A column that must point to an existing row in another table |
| **H2** | A small SQL database written in Java; a file locally, in memory in tests |
| **Hash** | The output of a one-way function; a password hash cannot be turned back into the password |
| **HMAC256** | The algorithm that signs the JWTs with the secret |
| **Idempotent** | Doing it twice has the same result as doing it once |
| **Issuer (`iss`)** | JWT claim: who created the token |
| **JWT** | JSON Web Token: a signed token that proves who the caller is |
| **Ktor** | JetBrains' Kotlin framework for web servers |
| **Lambda** | A function without a name, written in `{ }` |
| **`LIKE`** | SQL pattern matching; `%` matches any text, `_` any single character |
| **Mapper** | A function that converts between representations (row → model, model → DTO) |
| **Migration** | A versioned script that changes a database schema without losing data (Flyway); not used here |
| **Modular monolith** | One application, internally split into independent modules (features) |
| **Null safety** | Kotlin's rule that a value can only be `null` if its type ends with `?` |
| **Offset paging** | Paging with `limit` (page size) and `offset` (how many to skip) |
| **Owner** | The user a resource belongs to, such as `feedback.user_id` |
| **`PageResponse`** | The shape of every list: `{ items, total, limit, offset }` |
| **Permission** | A single right, such as `user.read`; defined in `enum class Permission` |
| **Plugin (Ktor)** | A feature installed in the server, such as authentication or JSON |
| **Query parameter** | A value in the URL after `?`, such as `?q=alice&limit=20` |
| **Rate limit** | A maximum number of requests per time period |
| **RBAC** | Role-Based Access Control: users get roles, roles grant permissions |
| **Repository** | The class that reads and writes one kind of data in the database |
| **`ResultRow`** | One row returned by an Exposed query |
| **Route** | Code that answers one URL and HTTP method |
| **Salt** | Random data added to a password before hashing, so equal passwords get different hashes |
| **Sealed class** | A class whose subclasses are all known to the compiler (`AppException`) |
| **Seeder** | Code that fills the database with required data at startup (`AuthorizationSeeder`) |
| **Service** | The class that holds the business rules of a feature |
| **Soft delete** | Marking a row as inactive instead of deleting it (users) |
| **SQLState 23505** | The standard SQL error code for a unique constraint violation |
| **StatusPages** | The Ktor plugin that turns exceptions into error responses |
| **Subject (`sub`)** | JWT claim: the user id |
| **Token version (`ver`)** | A number per user, copied into every token; raising it invalidates all earlier tokens |
| **Transaction** | A group of database operations that succeed or fail together |
| **Unique index** | A database rule that a column's values must all be different |
| **UUID** | A 128-bit random identifier such as `3f2c4a1e-...`; practically impossible to guess |
| **`val` / `var`** | A read-only / reassignable variable in Kotlin |
