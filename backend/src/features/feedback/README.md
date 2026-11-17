# Feedback

The **reference feature**: users write feedback about the app, and an admin reads it and changes its status. Copy this
feature when you add something that belongs to a user. It shows every common pattern in one place:

- the caller becomes the owner of what they create;
- access with `getOwnerId` + `requireOwnerOrPermission` (the owner may always, others need a permission);
- a field that only an admin may change (the status), on its own endpoint;
- a list with search, filter, sort and paging, where a regular user only sees their own items;
- a hard delete, and a foreign key to `users`.

Endpoints and permissions: `TECHNICAL_DESIGN.md` §4 and §6. Error codes: §5.

## Files

| File | What it does |
|---|---|
| `route/FeedbackRoutes.kt` | The six endpoints under `/feedback`. Each one reads the request, does one access check, calls the service and responds. No business rules |
| `service/FeedbackService.kt` | The rules: the message is trimmed and 1–2000 characters, a new item starts as `OPEN`, the server sets the id and `createdAt`. `getOwnerId(id)` gives the owner for the access check, or 404 |
| `repository/FeedbackRepository.kt` | The interface the service uses: find, search, count, save, `updateMessage`, `updateStatus`, delete. Each update writes only its own column, so an admin closing an item and the owner editing it at the same time cannot undo each other |
| `repository/ExposedFeedbackRepository.kt` | The real implementation with Exposed. `search` and `count` share `filtered(query)`, so the total always uses the same filters |
| `database/FeedbackTable.kt` | The `feedback` table. `owner_id` is a foreign key to `users`; the status is stored by its name |
| `model/Feedback.kt` | The domain object the service works with |
| `model/FeedbackStatus.kt` | `OPEN`, `IN_PROGRESS`, `CLOSED` |
| `model/FeedbackQuery.kt` | What a list request asks for: text, status, owner, sort and page. `null` means "no filter" |
| `model/FeedbackSortField.kt` | The fields a client may sort on, with the name used in the URL (`createdAt`, `status`) |
| `dto/FeedbackRequest.kt` | JSON body to create an item or change its message: `{ "message": ... }` |
| `dto/UpdateFeedbackStatusRequest.kt` | JSON body to change the status: `{ "status": ... }` |
| `dto/FeedbackResponse.kt` | The JSON the client receives |
| `mapper/FeedbackMapper.kt` | `Feedback.toResponse()`: model → JSON |
| `mapper/FeedbackDatabaseMapper.kt` | `ResultRow.toFeedback()`: database row → model |
| `mapper/FeedbackQueryMapper.kt` | `Parameters.toFeedbackQuery()`: query parameters → `FeedbackQuery`; an invalid value gives 400 |

## How a request flows

`PATCH /api/v1/feedback/{id}` with `{ "message": "..." }`:

1. `plugins/Authentication.kt` checks the token; without a valid one the client gets 401.
2. `FeedbackRoutes` reads the id (`uuidParameter`, 400 when invalid) and asks `feedbackService.getOwnerId(id)`
   (404 when unknown).
3. `requireOwnerOrPermission(currentUserId, ownerId, Permission.FEEDBACK_UPDATE)`: the owner passes, anyone else
   needs `feedback.update` (403 otherwise).
4. `call.receive<FeedbackRequest>()` reads the body; `feedbackService.updateMessage` validates it and calls
   `repository.updateMessage`, which writes only the message.
5. The route responds with `FeedbackResponse` (200).

The list (`GET /feedback`) is the one route that narrows instead of refusing: without `feedback.read`, the route sets
`ownerId` to the caller, whatever filter was sent.

## Copying it

The step-by-step version, for a feature without an owner: `docs/guide/08-tutorial-categories.md`. Tests:
`backend/test/features/feedback/` (fake repository, service, mapper, repository against H2, routes).
