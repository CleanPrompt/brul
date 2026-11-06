# Context for AI assistants

> **Students:** paste this file into your AI assistant (ChatGPT, Claude, Copilot Chat, ...) at the start of a
> conversation about this project, or add it to the assistant's project instructions. It makes the generated code fit
> the project. You remain responsible for understanding and checking every line.

---

**Assistant, read this first.** You are helping a student who is new to Kotlin, Ktor and Exposed. They must be able to
explain every line of the code in an assessment. Follow these rules strictly. When a rule conflicts with what you
would normally do, the rule wins. Verify Amper syntax, Ktor and Exposed APIs and library versions; when you are unsure
whether an API exists in the versions below, say so instead of guessing. Never invent APIs.

## 1. How to help

- Prefer simple, explicit, idiomatic code over clever code. No unnecessary generics, abstractions or patterns.
- Explain *why*, briefly, in plain language. Point to the file in the project that already does something similar.
- Copy existing patterns. The reference feature is `backend/src/features/feedback/`. For a global resource without an
  owner, follow the same structure with `requirePermission` only.
- Change only what is needed. Do not refactor unrelated code. Do not create empty folders or placeholder files.
- Never introduce a new library, framework or tool (Spring, Gradle, Koin, Flyway, Redis, Mockito, Lombok, ...)
  without saying clearly that it is a new dependency and asking first.
- All project content is in **English**: code, comments, KDoc, names, API paths, JSON fields, error and log
  messages, tests, configuration, README files, OpenAPI and diagrams.
- Documentation has **one source per fact** (see the table in `docs/README.md`). When a change touches a documented
  fact, update that source and point to it from elsewhere; do not copy the fact into a second file.

## 2. Stack and versions

| Part | Choice |
|---|---|
| Language | Kotlin, JVM 21 |
| Build | **JetBrains Amper** (`project.yaml`, `backend/module.yaml`). **Not Gradle, not Maven.** Run with `./kotlin run`, `./kotlin test` (Windows `kotlin.bat`) |
| Web | Ktor 3 (Netty engine), plugins: ContentNegotiation, StatusPages, Authentication JWT, RateLimit, CallLogging |
| Database | Exposed **0.61.0** DSL (not DAO), `exposed-java-time`, H2 |
| JSON | kotlinx.serialization |
| Security | Auth0 java-jwt (HMAC256), jBCrypt (cost 12) |
| Config | dotenv-kotlin; `backend/.env` locally, environment variables win |
| Tests | Kotlin Test, Ktor `testApplication` (test host), fakes; no mocking library |

Source layout (Amper): sources in `backend/src/`, resources in `backend/resources/`, tests in `backend/test/`.
There is **no** `src/main/kotlin`. Root package: `nl.brul.backend`.

## 3. Architecture rules

Feature-first modular monolith:

```text
backend/src/
├── Application.kt     # entry point, explicit dependency wiring (no DI framework)
├── config/            # AppConfig, DatabaseFactory
├── plugins/           # Ktor plugin configuration only, no business logic
├── shared/error/      # AppException, ErrorResponse
├── shared/http/       # uuidParameter() and the query parameter readers
├── shared/paging/     # Page, SortDirection, PageResponse<T>
├── shared/database/   # Exposed helpers shared by repositories (containsIgnoringCase, toSortOrder)
└── features/<name>/   # route, service, repository, database, model, dto, mapper
```

Each feature has only the folders it needs:

| Folder | Allowed | Not allowed |
|---|---|---|
| `route/` | read the request, check access, call the service, respond | business rules, Exposed code |
| `service/` | business rules, validation, generating ids and timestamps | `ApplicationCall`, `ResultRow`, Exposed code |
| `repository/` | an interface + `Exposed<Name>Repository`; returns domain models | HTTP, permissions |
| `database/` | Exposed `Table` objects | logic |
| `model/` | domain data classes and enums | `@Serializable` |
| `dto/` | `@Serializable` request/response classes | sensitive fields (password hashes, token versions) |
| `mapper/` | every conversion, as extension functions | database queries, permission checks |

- Wiring is manual in `backend/src/Application.kt` (constructor injection). Every new repository/service must be
  wired there **and** in `TestServices` in `backend/test/TestFixtures.kt`, and passed through `configureRouting` in
  `backend/src/plugins/Routing.kt` and `configureTestApp`.
- `plugins/` only configures Ktor; no business logic.
- `shared/` must not depend on any feature.

## 4. Security and authorization rules

- Write `authentication` and `authorization` in full in code and docs; never `auth` (only the URL prefix `/auth`).
- Tables: `users`, `roles`, `permissions`, `user_roles`, `role_permissions`. Permissions come only from roles; there
  are no direct user permissions.
- Permissions are `enum class Permission(val key: String)`; roles are `enum class DefaultRole(val permissions)`.
  ADMIN uses `Permission.entries`. `AuthorizationSeeder` stores both at startup. **Never check a permission with a
  string literal.** Roles and permissions are never created through the API.
- Protected routes are inside `authenticate(JWT_AUTHENTICATION)`. Each starts with exactly one access check from
  `AuthorizationService`, before the work:
  - `requirePermission(call.currentUserId(), Permission.X)`: the caller needs the permission.
  - `requireOwnerOrPermission(currentUserId, ownerId, Permission.X)`: the owner is always allowed, anyone else needs
    the permission. For a user account the account id is the owner id (`/users/{id}`), so the check comes before any
    lookup. For an item owned by a user, the owner comes from the service's `getOwnerId(id)` (404 when unknown),
    then the check.
- Do not write authorization logic in routes beyond calling these functions. Do not trust roles in the JWT; the
  database is the source of truth.
- Deny by default: missing/invalid token → 401, missing permission → 403.
- JWT payload contains only `sub` (user id), `ver` (token version), `iss`, `aud`, `iat`, `exp`. No personal data, no
  roles, no secrets.
- Never log passwords, tokens, secrets, request bodies or the `Authorization` header.
- Secrets come from environment variables or `.env`; `.env` is never committed.

## 5. Errors and REST

- Throw subclasses of `AppException` (`shared/error/AppException.kt`): `BadRequest` (400), `Unauthorized` (401),
  `Forbidden` (403), `NotFound` (404, code required), `Conflict` (409, code required). They are mapped in
  `plugins/StatusPages.kt`. Never build an error response in a route.
- Use `requireValid(condition) { "message" }` for input checks, **never** Kotlin's `require()` (that becomes a 500).
- Error body: `{ "code": "USER_NOT_FOUND", "message": "User not found" }`. Codes are stable `UPPER_SNAKE_CASE`. No stack
  traces or SQL in responses.
- Paths: `/api/v1/<plural-lowercase-resource>`. JSON: camelCase. Timestamps: ISO 8601 UTC (`Instant.toString()`).
  Ids: server-generated UUIDs, exposed as strings.
- Status codes: create → 201 with the resource; update → 200 with the resource; delete/actions → 204.

## 6. Lists

Every list endpoint returns `PageResponse<T>` (`items`, `total`, `limit`, `offset`) from `shared/paging/` and accepts
`q`, feature filters, `sort`, `order`, `limit` (1–100, default 20) and `offset`. Pattern:

1. `<Name>Query` data class and `<Name>SortField(val key: String)` enum in `model/`.
2. `fun Parameters.to<Name>Query(): <Name>Query` in `mapper/`, built from the readers in
   `shared/http/QueryParameters.kt`: `searchText`, `oneOf`, `booleanOrNull`, `uuidOrNull`, `sortDirection`,
   `page`. Invalid values throw 400; never fall back silently.
3. Repository `search(query)` and `count(query)`, sharing a private `filtered(query): Query` that starts with
   `Table.selectAll()` and adds one `andWhere { ... }` per non-null filter. Text search uses
   `column.containsIgnoringCase(text)` from `shared/database/QueryConditions.kt`. Sort with
   `.orderBy(column to direction.toSortOrder(), Table.id to SortOrder.ASC)`, then `.limit(n)` and `.offset(n.toLong())`.
4. Service `search(query): PageResponse<...>`.
5. The route narrows the query to what the caller may see (filters never widen access).

## 7. Kotlin style

- Official Kotlin style, 4 spaces, max ~120 characters per line.
- `PascalCase` classes, `camelCase` functions/properties, `UPPER_SNAKE_CASE` constants.
- Suffixes: `...Request`, `...Response`, `...Repository`, `Exposed...Repository`, `...Table`, `...Service`,
  `...Mapper` (file names), `...Test`. No `I` prefix on interfaces.
- Prefer `val` and immutable data classes (`copy` to change). **No `!!`. No wildcard imports.**
- Database names `snake_case`; Kotlin properties `camelCase`.
- KDoc only where it explains *why*; no comments that repeat the code.

## 8. Tests

- Kotlin Test (`@Test`, `assertEquals`, `assertFailsWith`). Test names in English between backticks, e.g.
  `` `returns 403 when permission is missing` ``.
- Route tests: `testApplication { application { configureTestApp(TestServices()) } ... }`. **Never call `module()` in
  tests**; it needs a real database.
- Helpers in `backend/test/TestFixtures.kt`: `testConfig()`, `TestServices`, `configureTestApp`, `postJson`,
  `register(email)`, `errorCode()`, `TEST_ADMIN_EMAIL` (registers as admin), `TEST_CREATED_AT`.
- Fakes (e.g. `backend/test/features/feedback/FakeFeedbackRepository.kt`) keep data in a map and must behave like the
  Exposed implementation, including refusing duplicates and keeping fields that `update` never changes.
- Repository tests use in-memory H2:
  `Database.connect("jdbc:h2:mem:<unique_name>;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")` and
  `SchemaUtils.create(...)` in `@BeforeTest`. The database is shared by all tests in the class, so every test must use
  its own data (a new owner, a unique name) and filter on it.
- Cover the happy flow and the error flows: 400, 401, 403, 404, 409.

## 9. Known pitfalls (verified in this project)

These cost real debugging time. Apply them without being asked.

**Exposed 0.61**
- `transaction { }` **retries failed transactions** (3 attempts by default). For inserts/updates that can hit a unique
  constraint, set `maxAttempts = 1` inside the transaction, catch `ExposedSQLException`, and map
  `exception.sqlState == "23505"` to `AppException.Conflict(...)`. Also check uniqueness in the service first, for a
  clear error in the normal case.
- Implicit joins across link tables (`A innerJoin B innerJoin C`) can fail with "no matching primary key/foreign key
  pair". Use explicit joins: `A.join(B, JoinType.INNER, A.col, B.col)`.
- Queries use `Table.selectAll().where { ... }` (the 0.61 API). `andWhere` mutates the query and returns it.
- `.limit(n)` takes an `Int`; `.offset(start)` takes a `Long`.
- `LIKE` with user input: use `LikePattern.ofLiteral(text)` (must run inside a transaction) so `%` and `_` match
  literally. `LikePattern("%") + LikePattern.ofLiteral(...)` throws because the escape characters differ; build
  `LikePattern("%${literal.pattern}%", literal.escapeChar)` instead, as `containsIgnoringCase` does.
- `deleteWhere { Table.id eq id }` needs `import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq`.
- Increments must be atomic: `it[col] = col + 1` inside `with(SqlExpressionBuilder) { ... }`, not read-modify-write.
- A function returning different column types needs the type `Column<*>`.
- `SchemaUtils.create` only creates **missing tables**. It never adds or changes columns. After changing an existing
  table, tell the student to stop the server and delete `data/brul*`. A new table needs no reset.

**Time**
- Truncate timestamps before saving: `Instant.now().truncatedTo(ChronoUnit.SECONDS)`. Otherwise the value read back
  from H2 differs from the one in memory and equality checks fail.

**Ktor**
- In `StatusPages`, `status(...)` is a member function of the configuration; do not import
  `io.ktor.server.plugins.statuspages.status` (it does not compile).
- `call.respond(PageResponse<FeedbackResponse>(...))` works with generics because `respond` is reified; keep the type
  visible.
- CallLogging uses `disableDefaultColors()`. Without it, Ktor loads the native Jansi library and newer JVMs print
  "restricted method" warnings at startup. Do not "fix" those warnings with `--enable-native-access`.
- `call.receive<T>()` throws `BadRequestException` for a missing or malformed body; `StatusPages` already maps it to 400.

**JWT and authentication**
- Every token must carry `ver`; `validate` rejects a token whose `ver` differs from `users.token_version`, or whose
  user is missing or inactive. Logout, deactivation and password changes call `incrementTokenVersion`.
- A wrong *current* password on password change is 400 `INVALID_CURRENT_PASSWORD`, not 401.
- Login returns the same `INVALID_CREDENTIALS` for unknown email, inactive user and wrong password.

**Manual testing (`backend/http/*.http`, IntelliJ HTTP client)**
- `{{baseUrl}}` stays unresolved until the environment `dev` is selected.
- Login and registration share a rate limit of 10 per minute; a full run of the files uses most of it. New requests
  should reuse stored tokens (`client.global.set(...)`) instead of logging in again. Rate-limited endpoints with a
  different purpose get their own limiter in `plugins/RateLimiting.kt`.
- `5-logout.http` invalidates the user's tokens; `1-authentication.http` must run again afterwards.
- Make request files repeatable: create throwaway data, or accept 409 where a resource may already exist.

**Configuration**
- `BOOTSTRAP_ADMIN_EMAIL` only works for an account registered *after* it is set. If the admin was registered before,
  delete `data/brul*` and register again.

## 10. Checklist before you answer with code

- [ ] Correct layer for every piece of code; conversions in `mapper/`.
- [ ] Access check first in every protected route, using the enum.
- [ ] Errors via `AppException`, validation via `requireValid`.
- [ ] Wired in `Application.kt`, `Routing.kt`, `TestServices` and `configureTestApp`.
- [ ] Tests for the happy flow and 400/401/403/404/409; the fake behaves like the real repository.
- [ ] No new dependencies, no `!!`, no wildcard imports, lines under ~120 characters.
- [ ] Told the student if a table changed (delete `data/brul*`), and which docs to update (`openapi.yaml`,
      `docs/implementation-plan.md`, the feature `README.md`).
