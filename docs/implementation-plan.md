# Implementation Plan — Base: Users, Authentication and RBAC

## Goal

Finish a solid, explainable foundation for the API: **database**, **users**, **authentication** and
**role-based access control (RBAC)**. Other domain features are added later on top of this base.

Guiding principle: keep the code simple enough to explain line by line during the assessment.

## Status

Phases 1–15 are done (132 tests). The base is complete: users, authentication, RBAC, the reference feature
`feedback`, search and paging on every list, and a learning guide (`docs/guide/`).

**Next:** profile images, then the `reports` feature. Still to decide: the optional admin password reset.

This document holds the decisions and the history. The current endpoints, permission matrix, error codes and data
model are in `TECHNICAL_DESIGN.md`; the root `README.md` says where each fact lives.

## Decisions

Every decision with the alternative that was considered and the reason. You should be able to defend each one, or
argue for the alternative.

Before you make a new decision, ask:

1. **Can I explain every line?** If not, it is too complex.
2. **Does it follow the existing pattern?** Copy `feedback` before inventing something new.
3. **Does it need a new library?** Then discuss it first. Every dependency is something everyone must understand.
4. **Write it down.** Add a row to the right table below: decision, alternative, reason.

### Architecture

| Decision | Alternative | Reason |
|---|---|---|
| Feature-first folders | Layer-first (`controllers/`, `services/`, ...) | Everything about one feature is together; a feature can be added or removed as a unit |
| Manual wiring in `Application.kt` (constructor injection) | A DI framework (Koin, Spring) | 30 readable lines, nothing to configure, nothing magic to explain |
| Repository interface + Exposed implementation | Services call Exposed directly | Services can be tested with fakes; database code stays in one layer |
| Separate DTOs and domain models | Send the models as JSON | A password hash can never leak; the JSON contract can change without touching the model |
| Mappers as extension functions, e.g. `user.toResponse()`, `row.toUser()` | Mapper classes, or conversion inside services | Short, idiomatic, easy to point out during the assessment |
| JetBrains Amper | Gradle | Much less build configuration for a single-module project |
| `AppConfig` with environment variables and `.env` | `application.yaml` | `application.yaml` was not read by `embeddedServer` and has been removed |
| `SchemaUtils.create`; delete `./data/brul*` after changing a table | Migrations (Flyway) | Matches the course material (H2 + Exposed); one tool less to learn |
| One `transaction {}` per repository method; database constraints guard races | Transactions across several methods | The unique index on email is mapped to 409 instead of adding transaction management |
| `TestServices` wires all services with fakes; `testApplication` calls the `configure...()` functions | A mocking library, or tests against a real database | Same wiring as `module()` without a database (course lesson 7) |
| Removed CallId / `requestId`, RequestValidation, Swagger UI | Keep them | Not needed for the base; `.http` files for manual testing, `openapi.yaml` as the API contract |

### Security

| Decision | Alternative | Reason |
|---|---|---|
| `authentication` (who is the caller) and `authorization` (what may the caller do) written in full; URL prefix stays `/auth` | `auth` everywhere | Matches the distinction from course lesson 6; `/auth` is the usual URL convention |
| JWT access tokens | Server-side sessions | Stateless and the usual choice for a mobile client |
| Logout with a `token_version` per user, sent as the `ver` claim and compared in `validate`; logout, password change and deactivation increment it | A blocklist of revoked tokens; refresh tokens | A JWT cannot be revoked on its own; this needs one column and no extra query. Trade-off: logout ends the sessions on every device |
| Permissions read from the database on every request | Roles inside the token | Revoking a role takes effect immediately, even for tokens already issued |
| `enum class Permission` in code, seeded into the `permissions` table | Free strings in the database | Every permission has code that checks it; a typo is a compile error |
| `enum class DefaultRole` (ADMIN, USER) is the permission matrix; the API only assigns and revokes roles | Creating roles through the API | A role created at runtime has no meaning without code that uses its permissions |
| Permissions only through roles | Direct user permissions (`user_permissions` table, removed) | Matches the role × action matrix (KISS) |
| Each protected route calls `requirePermission(...)` or `requireOwnerOrPermission(...)` inside `authenticate(JWT_AUTHENTICATION)` | A custom route plugin; a third check for user accounts | One service holds the rules (SRP); a plain function call is easier to explain; two checks are easier to choose between than three |
| For owned items the route reads the owner first (`getOwnerId`), then calls `requireOwnerOrPermission`; an unknown id gives 404 before the access check. User accounts follow the same order | Check access before the lookup | The owner is only known after a lookup; ids are random UUIDs, so this reveals nothing useful. One order everywhere is easier to learn than a rule with an exception |
| The owner column is `owner_id` (property `ownerId`), also in JSON and as a query filter | `user_id` / `userId` | `userId` could also mean "the caller"; `ownerId` matches the parameter of `requireOwnerOrPermission` |
| First admin through `BOOTSTRAP_ADMIN_EMAIL` | A seeded admin with a default password | No password in code or configuration; works on an empty database |
| Same error for unknown email, inactive user and wrong password | Specific errors | Callers cannot discover which emails are registered |
| BCrypt, cost 12 | Fast hashes (SHA-256) | Deliberately slow and salted: brute force becomes impractical |
| Rate limit on login, registration and password change | No limit | Slows down password guessing |

### API design

| Decision | Alternative | Reason |
|---|---|---|
| `/api/v1` prefix | No version | A breaking change can later go to `/api/v2` while v1 keeps working |
| UUIDs as ids, generated by the server; exposed in responses and URLs; `sub` in the JWT | Auto-increment numbers | Cannot be guessed or counted; clients never send an id when creating a resource |
| Throw `AppException` subtypes; map them in `StatusPages` to one shape `{ code, message }` | Error responses built in routes, different shapes per endpoint | Routes stay short; one consistent error format (course lesson 6); one piece of client code reads every error |
| `requireValid()` in services, thrown as `AppException.BadRequest` | Kotlin's `require()`; the RequestValidation plugin | `require()` becomes a 500; no extra plugin needed |
| Users deactivated, never deleted | Hard delete | History and references stay valid; an admin can reactivate |
| Feedback deleted for real | Soft delete | No history worth keeping; also shows the contrast with users |
| Reference feature `feedback`: user-owned CRUD with an admin override | Use the `user` feature as the example | `user` is an exception (created via `/auth/register`, soft delete); developers need a plain example to copy |
| Feedback status on its own endpoint `PATCH /feedback/{id}/status` (`feedback.update`) | A status field in `PATCH /feedback/{id}` | Each route has exactly one access check; a combined body would need a different check per field |
| Wrong current password → 400 `INVALID_CURRENT_PASSWORD` | 401 | The token is valid; a client treats 401 as "session expired" |

### Lists

| Decision | Alternative | Reason |
|---|---|---|
| Query parameters on the existing list endpoints (`GET /users`, `GET /feedback`) | One global `/search` endpoint | The feature that owns the data knows its filters and its access rules |
| `LOWER(column) LIKE '%text%'` with `%` and `_` escaped (`containsIgnoringCase`) | Full-text search (Lucene, H2 fulltext) | Enough for this amount of data; full-text search is a new technology |
| `limit` (default 20, max 100) and `offset`; response `PageResponse<T>` with `items`, `total`, `limit`, `offset` | Cursor paging; a bare JSON array | Offset paging is the easiest to explain; `total` lets a client show "page 2 of 5" |
| `sort` from a fixed list per feature (enum with a `key`), `order=asc\|desc`; the id is always a second sort key | Any column name | Only known columns can be sorted on; the extra id key keeps pages stable when values are equal |
| Filters only narrow a result; `GET /feedback` forces `ownerId` to the caller without `feedback.read` | Trust the filter | A filter must never show more than the caller may see |
| Invalid parameter → 400 with the parameter name | Silently use a default | A typo such as `limit=1O` should be noticed, not hidden |

### Documentation

| Decision | Alternative | Reason |
|---|---|---|
| One source per fact (table in the root `README.md`); `CLAUDE.md` imports the shared rules from `docs/guide/llm-context.md` | Copy endpoints, permissions and rules where they are needed | They were copied into four to six files and drifted apart; one source means one place to update |
| Each kind of document has one job: the guide explains concepts, `TECHNICAL_DESIGN.md` lists, the feature READMEs describe their code | The same explanation in several documents | The same topics were explained in up to four places; students did not know where to start |
| Every feature and `shared/` has a `README.md` that explains what its code does, file by file; other source folders have none (the root `README.md` describes them) | A short pointer `README.md` in every folder | Fellow students read the code without the author next to them; three-line pointers did not explain enough |
| Diagrams in Mermaid inside the Markdown | PlantUML files | GitHub and IntelliJ render Mermaid without a plugin, and the diagram sits next to its text |

## Phases

### Phase 1 — Errors, mappers and logging ✅

Sealed `AppException`, `ErrorResponse` everywhere, `requireValid()`, login throws instead of returning
`null`, 409 on a duplicate email, mappers as extension functions, call logging without secrets.

### Phase 2 — Database and seeding ✅

JDBC driver from `AppConfig`, `enum class Permission`, `enum class DefaultRole`, `AuthorizationSeeder`,
`BOOTSTRAP_ADMIN_EMAIL`, `created_at` on `users` (via `exposed-java-time`, ISO 8601 UTC in the API).

### Phase 3 — Authorization integration ✅

All repositories and services wired in `Application.kt`; `call.currentUserId()`;
`AuthorizationService.requirePermission` and `requireAccessToUser` (merged into `requireOwnerOrPermission` in phase 13); `removeRoleFromUser`.
Fixed an existing bug: the permission query joined `user_roles` and `role_permissions` without a join column.

### Phase 4 — User feature ✅

`GET /users/me`, `GET /users`, `GET|PATCH|DELETE /users/{id}` behind `authenticate(JWT_AUTHENTICATION)`.

### Phase 5 — Role routes ✅

`GET /roles`, `GET|POST /users/{id}/roles`, `DELETE /users/{id}/roles/{roleId}`; `RoleResponse`,
`AssignRoleRequest`.

### Phase 6 — Finish authentication ✅

New users get `USER`; the bootstrap admin also gets `ADMIN`.

### Phase 7 — Tests and documentation ✅

65 tests: services, mappers, JWT (invalid signature, wrong issuer, expired), repositories against H2,
and API tests for authentication, users and roles (401/403/404, own versus other account, role revoked
while the token stays the same, deactivated user's token). Updated `.http` files, `openapi.yaml`,
READMEs, `TECHNICAL_DESIGN.md` and the architecture diagram.

### Phase 8 — Logout with token version ✅

- `token_version` column on `users` (default 0), `User.tokenVersion`, not exposed in `UserResponse`.
- `UserRepository.incrementTokenVersion(id)` as one atomic SQL update (`token_version = token_version + 1`).
- `JwtService.generateToken(userId, tokenVersion)` adds the `ver` claim.
- `configureAuthentication` rejects a token whose `ver` is missing or differs from the database (401).
- `POST /api/v1/auth/logout` (authenticated, not rate limited) increments the version and returns 204.
- `UserService.deactivate` also increments the version, so old tokens stay invalid after reactivation.
- Tests: token rejected after logout, new login works, logout without token returns 401.

### Phase 9 — Reactivate a user ✅

- `POST /api/v1/users/{id}/reactivate` requires `user.delete` (whoever may deactivate may undo it); returns 204.
- `UserService.reactivate` sets `active` back to `true` and leaves the token version alone: deactivation already
  raised it, so tokens from before the deactivation stay invalid and the user has to log in again.
- Reactivating an active user changes nothing (still 204); no extra rule, because the outcome is the same.
- Tests: active again with the same token version, 404 for an unknown user, and API tests for 204, 401, 403,
  404, 400 and "old token rejected, new login works". `2-users.http` reactivates the throwaway user.

### Phase 10 — Change password ✅

- `POST /api/v1/users/me/password` with `currentPassword` and `newPassword`; the current password is required, so a
  stolen token alone cannot change it. Same length rules as registration; increments the token version.
- Returns 200 with a new `AuthenticationResponse`: every other device is logged out, the device that changed the
  password keeps working without logging in again.
- A wrong current password gives 400 `INVALID_CURRENT_PASSWORD`, not 401: the token itself is valid, and a client
  treats 401 as "session expired". The new password must differ from the current one.
- The logic sits in `AuthenticationService` (it owns `PasswordHasher` and the password rules); the URL stays under
  `/users/me` because it changes the caller's own account.
- Rate limited separately (5 per minute), so guessing the current password with a stolen token is slow.
- Tests: new token works and the old one gets 401, old password no longer logs in, wrong current password,
  short or unchanged new password, 401 without token, 429. `2-users.http` changes the throwaway user's password.

### Phase 11 — Reference feature: feedback ✅

User-owned CRUD that shows every common pattern in one feature (endpoints in `TECHNICAL_DESIGN.md` §6).

- `feedback` table: `id`, `user_id` (foreign key to `users`), `message` (1–2000 characters), `status`
  (`enum class FeedbackStatus`: `OPEN`, `IN_PROGRESS`, `CLOSED`, stored by name) and `created_at`.
- `Permission.FEEDBACK_READ`, `FEEDBACK_UPDATE` and `FEEDBACK_DELETE`; ADMIN gets them through
  `Permission.entries`, USER gets none.
- `AuthorizationService.requireOwnerOrPermission(currentUserId, ownerId, permission)`: owners always have access,
  others need the permission.
- The status has its own endpoint (`PATCH /feedback/{id}/status`), so each route has one access check. Only
  `feedback.update` may change it, also for the owner.
- Delete is a hard delete, in contrast with users.
- Tests (27 new): service, mapper, repository against H2 (including the foreign key), route tests for 201, 200, 204,
  400, 401, 403 and 404, and `requireOwnerOrPermission`. `4-feedback.http` (logout moved to `5-logout.http`).
- `features/feedback/README.md` explains how to copy the feature.

### Phase 12 — Search, filter, sort and paging ✅

Applied to the two lists that exist, `GET /users` and `GET /feedback`, as the pattern for future domain features.

The parameters per list are in `TECHNICAL_DESIGN.md` §6.

- `shared/http/QueryParameters.kt`: `searchText`, `oneOf`, `booleanOrNull`, `uuidOrNull`, `sortDirection`,
  `pageRequest`; every invalid value gives 400 with the parameter name.
- `shared/paging/`: `PageRequest`, `SortDirection`, `PageResponse<T>`. `shared/database/QueryConditions.kt`:
  `containsIgnoringCase` and `SortDirection.toSortOrder()`.
- Per feature: `<Name>Query` and `<Name>SortField` in `model/`, `Parameters.to<Name>Query()` in `mapper/`,
  `search(query)` + `count(query)` in the repository (replacing `findAll`/`findByUserId`), `search(query)` in the
  service returning `PageResponse`.
- The Exposed repository starts from `selectAll()` and adds one `andWhere` per filter that is set; `count()` uses the
  same filters without the page.
- Breaking change: both lists now return `{ items, total, limit, offset }` instead of a plain array.
- Tests (17 new): query parameter readers, repository search against H2 (filters, case, literal `%`, sort, page),
  route tests for filters, sorting, paging, 400 on every invalid parameter, 403, and "userId cannot widen a
  regular user's search". `.http` files search as well.
- Learning guide for students in `docs/guide/`: 15 chapters, a multiple-choice quiz and `llm-context.md` for AI
  assistants. Its `categories` tutorial code was compiled and tested before it was published.

### Phase 13 — Simplify ✅

- One access check less: `requireAccessToUser` only called `requireOwnerOrPermission`, so the routes now call
  `requireOwnerOrPermission` directly (users own their account).
- `CreateFeedbackRequest` and `UpdateFeedbackRequest` were identical; they are now one `FeedbackRequest`, like
  `CategoryRequest` in the tutorial.
- `PageRequest` became `Page` (and `Parameters.pageRequest()` became `page()`): the `...Request` suffix is reserved for
  DTOs that the client sends as JSON, and `Page` is a model read from query parameters.
- Documentation: one source per fact (table in `docs/README.md`, moved to the root `README.md` in phase 15). The
  endpoint, permission and error tables moved out of this plan into `TECHNICAL_DESIGN.md`; `CLAUDE.md` imports
  `docs/guide/llm-context.md` instead of repeating its rules; the folder READMEs and the root `README.md` became
  short pointers.

### Phase 14 — One owner rule ✅

- Every `requireOwnerOrPermission` call reads the owner first. `UserService.getOwnerId(id)` returns the account id,
  or 404 for an unknown user; `GET`/`PATCH /users/{id}` and `GET /users/{id}/roles` use it, so a regular user now
  gets 404 instead of 403 for an unknown id. `roleRoutes` receives `UserService` for this.
- Feedback: `user_id` became `owner_id` and `userId` became `ownerId` in the model, the JSON response and the
  `GET /feedback` filter (breaking change; no client exists yet). The `feedback` table changed: delete `data/brul*`.
- Tests (4 new): `getOwnerId`, 404 for an unknown user and for the roles of an unknown user, 403 for another user's
  roles.

### Phase 15 — Documentation restructure ✅

- One job per kind of document: the guide explains, `TECHNICAL_DESIGN.md` lists, the feature READMEs describe their
  code. Explanations that appeared in up to four places now appear once.
- The guide went from 15 to 10 chapters: the Kotlin and library primers, architecture and the life of a request,
  authentication and authorization, and errors and the database were merged. Chapter 13 (design decisions) became the
  *Alternative* column of the decisions tables above.
- The root `README.md` is the only entry point: it absorbed `backend/README.md` and the one-source-per-fact table of
  `docs/README.md`. The pointer READMEs in `config/`, `plugins/`, `features/` and `test/` were removed.
- `TECHNICAL_DESIGN.md` only lists: the architecture and data model are Mermaid diagrams (the PlantUML file was
  removed), and the explanations of layers, lists and tests point to the guide.
- Every feature README and `shared/README.md` explains its code file by file.

### Optional

- Admin password reset (the only recovery path without an email flow); still to decide.

## Out of scope

| Left out | Why | When you would add it |
|---|---|---|
| Domain features beyond the reference feature | The base comes first | Now: profile images and reports are next |
| Database migrations (Flyway) | An extra tool to learn; `SchemaUtils.create` is enough during development | As soon as there is a production database whose data must survive schema changes |
| Refresh tokens | More endpoints and storage | When users should stay logged in for weeks without re-entering a password |
| Email verification, password reset by email | Needs an email service | For a public app |
| Creating roles or permissions through the API; direct user permissions | See the security decisions | Not planned |
| Organization-scoped roles and object-level permissions beyond "own item" | Not needed for one neighbourhood app | When users act on behalf of a group |
| Swagger UI, CallId, RequestValidation, ktlint/detekt | Extra dependencies | `openapi.yaml` already documents the contract |
| PostgreSQL | H2 needs no installation | For production; only `repository/` code and configuration would change |
