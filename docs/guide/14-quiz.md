# 14. Quiz

Multiple-choice questions per chapter. Choose an answer, then open **Answer** to check it and read why.

---

## Chapter 2: Kotlin

**2.1** What does `val user: User? = repository.findById(id)` tell you?

- A. `user` can be reassigned later
- B. `user` may be `null`
- C. `findById` throws when the user does not exist
- D. `User` is a nullable class

<details><summary>Answer</summary>

**B.** The `?` after the type means the value may be `null`. `val` (not `var`) means it cannot be reassigned, so A is
wrong. The `?` belongs to this variable's type, not to the class (D).
</details>

**2.2** What does `repository.findById(id) ?: throw AppException.NotFound(...)` do?

- A. Always throws
- B. Throws only when `findById` throws
- C. Returns the found value, or throws when it is `null`
- D. Returns `null` when the user does not exist

<details><summary>Answer</summary>

**C.** `?:` (Elvis) uses the right side only when the left side is `null`. This is the project's standard way of
saying "find it, or fail with 404".
</details>

**2.3** Why does `toHttpStatus()` in `StatusPages.kt` not need an `else` branch?

- A. Kotlin never requires `else`
- B. `AppException` is a `sealed class`, so the compiler knows all subclasses
- C. Unknown exceptions are ignored
- D. `when` always returns 500 by default

<details><summary>Answer</summary>

**B.** Because the class is sealed, the compiler can check that every subclass is handled. Adding a subclass makes this
code fail to compile until it is handled, which is exactly what you want.
</details>

**2.4** What is `fun User.toResponse(): UserResponse`?

- A. A method inside the `User` class
- B. An extension function: it adds `toResponse()` to `User` without changing the class
- C. A constructor of `UserResponse`
- D. A lambda

<details><summary>Answer</summary>

**B.** Extension functions are defined outside the class. All mappers in the project are written this way.
</details>

**2.5** `val updated = user.copy(displayName = "Alicia")`. What happens to `user`?

- A. Its display name becomes "Alicia"
- B. Nothing: `copy` creates a new object with one value changed
- C. It is deleted
- D. It throws, because `User` is immutable

<details><summary>Answer</summary>

**B.** Data classes with only `val` properties are immutable. `copy` makes a changed copy; the original stays the same.
</details>

---

## Chapter 3: Libraries

**3.1** Which library turns `UserResponse` into JSON?

- A. Exposed
- B. kotlinx.serialization (through Ktor's ContentNegotiation plugin)
- C. Auth0 java-jwt
- D. H2

<details><summary>Answer</summary>

**B.** Classes marked `@Serializable` are converted by kotlinx.serialization. Exposed is for the database, java-jwt for
tokens, H2 is the database.
</details>

**3.2** Where are the project's dependencies listed?

- A. `build.gradle.kts`
- B. `pom.xml`
- C. `backend/module.yaml`
- D. `Application.kt`

<details><summary>Answer</summary>

**C.** The project uses Amper, not Gradle or Maven.
</details>

**3.3** What does `call.receive<UpdateUserRequest>()` do when the body is not valid JSON for that class?

- A. Returns `null`
- B. Returns an empty object
- C. Throws; the StatusPages plugin answers 400
- D. Crashes the server

<details><summary>Answer</summary>

**C.** Ktor throws a `BadRequestException`, and `StatusPages` maps it to 400 `INVALID_REQUEST`.
</details>

**3.4** Why do domain models such as `User` not have `@Serializable`?

- A. Exposed does not allow it
- B. So they can never be sent to the client by accident; only DTOs are serializable
- C. It makes the application faster
- D. Kotlin forbids it on data classes

<details><summary>Answer</summary>

**B.** `User` contains `passwordHash`. Only DTOs, which contain exactly what the client may see, can become JSON.
</details>

---

## Chapter 4: Architecture

**4.1** A business rule changes: feedback messages may now be 5000 characters. Which layer changes first?

- A. `route/`
- B. `service/` (and the column length in `database/`)
- C. `dto/`
- D. `plugins/`

<details><summary>Answer</summary>

**B.** Validation is a business rule and lives in the service. Because the column is `varchar(2000)`, the table
definition must change too (and with it the local database, see chapter 9).
</details>

**4.2** Which layer is allowed to use `ResultRow`?

- A. Every layer
- B. Only `route/`
- C. Only `repository/` (through the database mappers)
- D. Only `service/`

<details><summary>Answer</summary>

**C.** The repository converts rows into domain models; no other layer sees database types.
</details>

**4.3** What is constructor injection?

- A. A class creates its own dependencies
- B. A class receives its dependencies through its constructor
- C. A framework scans annotations
- D. A security attack

<details><summary>Answer</summary>

**B.** `class UserService(private val repository: UserRepository)`. `Application.kt` creates the objects and passes them
in; tests pass in fakes the same way.
</details>

**4.4** Why do tests not call `Application.module()`?

- A. It is private
- B. It creates the Exposed repositories, which need a real database
- C. It starts a real network server
- D. It deletes the test data

<details><summary>Answer</summary>

**B.** Tests use `configureTestApp(TestServices())`, the same wiring with fakes.
</details>

---

## Chapter 5: Life of a request

**5.1** A request has no `Authorization` header and goes to `GET /api/v1/feedback/{id}`. Which code runs?

- A. The route handler, which throws 401
- B. None of the feedback code: the authentication plugin rejects it with 401 first
- C. The service, which throws 404
- D. The repository

<details><summary>Answer</summary>

**B.** The route is inside `authenticate(JWT_AUTHENTICATION)`; without a valid token the handler never runs.
</details>

**5.2** A normal user requests another user's feedback item that exists. What is the answer?

- A. 200
- B. 401
- C. 403
- D. 404

<details><summary>Answer</summary>

**C.** The user is authenticated (so not 401), the item exists (so not 404), but they are not the owner and lack
`feedback.read`.
</details>

**5.3** The same user requests a feedback id that does not exist. What is the answer?

- A. 200
- B. 401
- C. 403
- D. 404

<details><summary>Answer</summary>

**D.** For owned items the owner is read first (`getOwnerId`), so an unknown id gives 404 before the access check.
</details>

**5.4** A route contains a bug that throws a `NullPointerException`. What does the client see?

- A. The stack trace
- B. 500 with `{ "code": "INTERNAL_ERROR", "message": "Internal server error" }`
- C. 400
- D. The connection is closed without an answer

<details><summary>Answer</summary>

**B.** `StatusPages` catches every `Throwable`, logs the details on the server and sends a generic 500.
</details>

---

## Chapter 6: Authentication

**6.1** Why does the database store a hash instead of the password?

- A. Hashes are shorter
- B. If the database leaks, the passwords themselves are not revealed
- C. BCrypt makes logging in faster
- D. Exposed cannot store strings

<details><summary>Answer</summary>

**B.** A hash cannot be turned back into the password. BCrypt is also slow and salted, so guessing is expensive.
</details>

**6.2** Which of these is in the JWT payload of this project?

- A. The password hash
- B. The user's roles
- C. The user id (`sub`) and the token version (`ver`)
- D. The `JWT_SECRET`

<details><summary>Answer</summary>

**C.** The payload is readable by anyone, so it holds no secrets and no roles. Roles are read from the database.
</details>

**6.3** What happens to a user's old tokens after `POST /auth/logout`?

- A. Nothing until they expire
- B. They are deleted from a token table
- C. They stop working, because the token version in the database went up
- D. Only the token used for the logout stops working

<details><summary>Answer</summary>

**C.** Every token carries the version it was made with; validation compares it with the database. Trade-off: all
devices are logged out.
</details>

**6.4** Why does login give the same error for an unknown email and a wrong password?

- A. To keep the code short
- B. So nobody can find out which emails are registered
- C. Because the database cannot tell the difference
- D. It is required by JWT

<details><summary>Answer</summary>

**B.** Different errors would let an attacker test which emails have an account.
</details>

**6.5** Changing your password with a wrong current password gives 400, not 401. Why?

- A. 401 does not exist in Ktor
- B. The token is valid; a client treats 401 as "session expired"
- C. 400 is faster
- D. Because the rate limit was hit

<details><summary>Answer</summary>

**B.** 401 means "we don't know who you are". Here we do; only the input is wrong.
</details>

---

## Chapter 7: Authorization

**7.1** Where does the server get a user's permissions on each request?

- A. From the JWT
- B. From the database, through the user's roles
- C. From `.env`
- D. From the request body

<details><summary>Answer</summary>

**B.** That is why removing a role takes effect immediately, even for a token issued before.
</details>

**7.2** What is the difference between 401 and 403?

- A. There is none
- B. 401: not authenticated; 403: authenticated but not allowed
- C. 401: not allowed; 403: not authenticated
- D. 403 is only for admins

<details><summary>Answer</summary>

**B.**
</details>

**7.3** A new feature needs a permission. Which is correct?

- A. Check `if (permissions.contains("report.read"))` in the route
- B. Add `REPORT_READ("report.read")` to `Permission` and call `requirePermission(..., Permission.REPORT_READ)`
- C. Create the permission through an API endpoint
- D. Put it in the JWT

<details><summary>Answer</summary>

**B.** Always the enum, never a string literal; the seeder stores it at startup.
</details>

**7.4** Which check fits `DELETE /api/v1/feedback/{id}`?

- A. `requirePermission(..., FEEDBACK_DELETE)`: only admins may delete
- B. `requireOwnerOrPermission(currentUserId, ownerId, FEEDBACK_DELETE)`
- C. No check: everyone may delete
- D. `requireOwnerOrPermission(currentUserId, id, FEEDBACK_DELETE)` with `id` from the URL

<details><summary>Answer</summary>

**B.** The owner may delete their own item; anyone else needs the permission. D compares the user id with the
*feedback* id, which never matches: the owner id must come from `feedbackService.getOwnerId(id)`. (For a user account
the URL id would be right, because users own their account.)
</details>

**7.5** Why can an admin not remove their own `ADMIN` role?

- A. It would crash the seeder
- B. So the system always keeps at least one admin
- C. Admins have no roles
- D. Exposed does not support it

<details><summary>Answer</summary>

**B.** Only another admin can take the role away, and that admin remains.
</details>

---

## Chapter 8: Errors and validation

**8.1** How should a service reject a display name of 500 characters?

- A. `call.respond(HttpStatusCode.BadRequest, "Too long")`
- B. `require(name.length <= 120) { "..." }`
- C. `requireValid(name.length <= 120) { "..." }`
- D. `return null`

<details><summary>Answer</summary>

**C.** `requireValid` throws `AppException.BadRequest` (400). `require` throws `IllegalArgumentException`, which would
become a 500. A service has no `call`.
</details>

**8.2** What is the purpose of the `code` field in an error?

- A. It is the HTTP status code
- B. A stable, machine-readable identifier that clients can check
- C. A stack trace
- D. A translation key

<details><summary>Answer</summary>

**B.** For example `EMAIL_ALREADY_REGISTERED`. It never changes; `message` may.
</details>

**8.3** Which status does `AppException.Conflict` produce?

- A. 400
- B. 404
- C. 409
- D. 500

<details><summary>Answer</summary>

**C.**
</details>

---

## Chapter 9: Database

**9.1** You add a column to `UsersTable`. The server starts but requests fail. Why?

- A. Exposed does not support new columns
- B. `SchemaUtils.create` does not change existing tables; delete `data/example*` locally
- C. The column needs `@Serializable`
- D. The JWT secret changed

<details><summary>Answer</summary>

**B.** It only creates missing tables. A migration tool would solve this properly; this project resets the local
database instead.
</details>

**9.2** Why does `ExposedUserRepository.save` use `maxAttempts = 1`?

- A. To make inserts faster
- B. Exposed retries failed transactions; a duplicate email can never succeed on a retry
- C. H2 only allows one attempt
- D. To disable transactions

<details><summary>Answer</summary>

**B.** Without it, the duplicate insert would be tried three times before failing.
</details>

**9.3** The service already checks whether an email exists. Why does the table also have a unique index?

- A. It is required by Exposed
- B. Two requests at the same moment could both pass the service check; the index stops the second insert
- C. Indexes make the check unnecessary in the service
- D. To sort users by email

<details><summary>Answer</summary>

**B.** The service gives a clear error in the normal case; the database is the final guard against the race.
</details>

**9.4** Why is the token version increased with one SQL statement (`token_version = token_version + 1`)?

- A. It is shorter
- B. Reading, adding and writing in Kotlin could lose an update when two requests run at the same time
- C. Exposed cannot read integers
- D. To avoid a transaction

<details><summary>Answer</summary>

**B.** The single statement is atomic.
</details>

---

## Chapter 10: Lists

**10.1** `GET /api/v1/users?limit=10&offset=20` returns which matches?

- A. 1 to 10
- B. 11 to 20
- C. 21 to 30
- D. 20 to 30

<details><summary>Answer</summary>

**C.** Skip 20, then take 10.
</details>

**10.2** A response has `total: 45`, `limit: 20`, `offset: 40` and 5 items. Is there a next page?

- A. Yes
- B. No: `40 + 5 = 45`, which is not less than `total`
- C. Only if `limit` is increased
- D. Cannot be known

<details><summary>Answer</summary>

**B.** There is a next page while `offset + items.size < total`.
</details>

**10.3** A normal user calls `GET /api/v1/feedback?userId=<someone else>`. What do they get?

- A. The other user's items
- B. 403
- C. Only their own items: the route overwrites `userId`
- D. 400

<details><summary>Answer</summary>

**C.** Filters may only narrow the result, never widen what the caller may see.
</details>

**10.4** Why does every search also sort on the id?

- A. Ids are alphabetical
- B. So the order is unique and pages never overlap when sort values are equal
- C. The database requires it
- D. To hide the id

<details><summary>Answer</summary>

**B.**
</details>

**10.5** `GET /api/v1/feedback?limit=500` gives?

- A. 500 items
- B. 100 items (the maximum)
- C. 20 items (the default)
- D. 400 `INVALID_REQUEST`

<details><summary>Answer</summary>

**D.** Invalid parameters are rejected instead of silently replaced, so mistakes are noticed.
</details>

**10.6** Why are `%` and `_` escaped in the search text?

- A. They are not allowed in JSON
- B. In `LIKE` they are wildcards; without escaping, a search for `50%` would also match "50 people"
- C. To prevent 500 errors
- D. H2 does not support them

<details><summary>Answer</summary>

**B.**
</details>

---

## Chapter 11: Testing

**11.1** What is a fake repository?

- A. A mock that only records calls
- B. A simple working implementation of the repository interface that keeps data in memory
- C. A copy of the production database
- D. A repository that always throws

<details><summary>Answer</summary>

**B.** It behaves like the real repository, without a database.
</details>

**11.2** The real repository refuses duplicate names, the fake does not. What is the risk?

- A. None
- B. Tests for the 409 cannot work, and tests may pass while the real application behaves differently
- C. The tests become slower
- D. The fake will not compile

<details><summary>Answer</summary>

**B.** A fake must behave like the real implementation.
</details>

**11.3** Which test proves that the SQL of a search is correct?

- A. A route test with `TestServices`
- B. A service test with a fake
- C. A repository test against in-memory H2
- D. A mapper test

<details><summary>Answer</summary>

**C.** Only repository tests run the real Exposed code.
</details>

**11.4** In `FeedbackRepositoryTest`, why does each test create a new owner and filter on it?

- A. Owners are required by H2
- B. The in-memory database is shared by all tests in the class (`DB_CLOSE_DELAY=-1`), so other tests' data is still
  there
- C. To test the foreign key
- D. For speed

<details><summary>Answer</summary>

**B.** Filtering on the test's own data keeps tests independent.
</details>

---

## Chapter 12: Building a feature

**12.1** You added `CategoriesTable` and its routes, but the tests do not compile. What did you most likely forget?

- A. A README
- B. Wiring the service in `TestServices` and passing it to `configureRouting` in `configureTestApp`
- C. The `.http` file
- D. Deleting the database

<details><summary>Answer</summary>

**B.** `configureRouting` got a new parameter; every caller, including the test fixture, must pass it.
</details>

**12.2** Do you have to delete `data/example*` after adding the new `categories` table?

- A. Yes, always after any change
- B. No: a new table is created at startup; only changed existing tables need a reset
- C. Only on Windows
- D. Only if the table has a unique index

<details><summary>Answer</summary>

**B.**
</details>

**12.3** Which status should `POST /categories` return for a name that already exists?

- A. 400
- B. 403
- C. 404
- D. 409

<details><summary>Answer</summary>

**D.** A conflict with existing data, code `CATEGORY_NAME_TAKEN`.
</details>

**12.4** Does `ADMIN` get `category.manage` automatically?

- A. No, it must be added by hand to the database
- B. Yes, `DefaultRole.ADMIN` contains `Permission.entries` and the seeder runs at startup
- C. Only after re-registering the admin
- D. Only if the JWT is renewed

<details><summary>Answer</summary>

**B.** And because permissions are read per request, the admin can use it with their current token.
</details>

---

## Chapter 13: Design decisions

**13.1** Why does the API not create roles at runtime?

- A. Exposed cannot insert roles
- B. A new role means nothing without code that checks its permissions
- C. Roles are stored in the JWT
- D. It would be too slow

<details><summary>Answer</summary>

**B.**
</details>

**13.2** Why was the list format changed to `{ items, total, ... }` before the Android app existed?

- A. JSON arrays are not allowed
- B. It is a breaking change; without clients it costs nothing, later it would need a new API version or a forced app
  update
- C. Ktor requires objects
- D. To make responses smaller

<details><summary>Answer</summary>

**B.**
</details>

**13.3** Which is a valid reason to add Flyway later?

- A. To make tests faster
- B. A production database whose data must survive schema changes
- C. To replace Exposed
- D. To store tokens

<details><summary>Answer</summary>

**B.** During development, resetting the local database is enough.
</details>
