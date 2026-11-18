# Feedback

The **reference feature**: users write feedback about the app, and an admin reads it and changes its status. Copy this
feature when you add something that belongs to a user. It shows every common pattern in one place: the caller becomes
the owner, access with `getOwnerId` + `requireOwnerOrPermission`, an admin-only field on its own endpoint, a list
that narrows to the caller's own items, one repository function per changed column, and a hard delete.

Endpoints and permissions: `TECHNICAL_DESIGN.md` §4 and §6. Error codes: §5.

## Class diagram

The service only knows the `FeedbackRepository` interface: `AppServices` gives it the Exposed implementation, the
tests give it `FakeFeedbackRepository`. Mappers and `FeedbackSortField` are left out; see *Files*.

```mermaid
classDiagram
    direction LR

    class FeedbackRoutes {
        <<routes>>
        +feedbackRoutes(feedbackService, authorizationService)
    }
    class AuthorizationService {
        +hasPermission(userId, permission) Boolean
        +requirePermission(userId, permission)
        +requireOwnerOrPermission(currentUserId, ownerId, permission)
    }
    class FeedbackService {
        +create(ownerId, request) FeedbackResponse
        +search(query) PageResponse~FeedbackResponse~
        +getById(id) FeedbackResponse
        +getOwnerId(id) UUID
        +updateMessage(id, request) FeedbackResponse
        +updateStatus(id, request) FeedbackResponse
        +delete(id)
    }
    class FeedbackRepository {
        <<interface>>
        +findById(id) Feedback
        +search(query) List~Feedback~
        +count(query) Long
        +save(feedback) Feedback
        +updateMessage(id, message) Boolean
        +updateStatus(id, status) Boolean
        +delete(id) Boolean
    }
    class ExposedFeedbackRepository {
        -filtered(query) Query
    }
    class FeedbackTable {
        <<object>>
        id
        owner_id
        message
        status
        created_at
    }
    class Feedback {
        +id: UUID
        +ownerId: UUID
        +message: String
        +status: FeedbackStatus
        +createdAt: Instant
        +MAX_MESSAGE_LENGTH$ = 2000
    }
    class FeedbackStatus {
        <<enumeration>>
        OPEN
        IN_PROGRESS
        CLOSED
    }
    class FeedbackQuery {
        +text: String
        +status: FeedbackStatus
        +ownerId: UUID
        +page: Page
    }
    class FeedbackRequest {
        <<dto>>
        +message: String
    }
    class FeedbackResponse {
        <<dto>>
        +id: String
        +ownerId: String
        +message: String
        +status: String
        +createdAt: String
    }

    FeedbackRoutes --> FeedbackService : calls
    FeedbackRoutes --> AuthorizationService : access check
    FeedbackRoutes ..> FeedbackRequest : receives
    FeedbackService --> FeedbackRepository : uses
    FeedbackRepository <|.. ExposedFeedbackRepository : implements
    ExposedFeedbackRepository --> FeedbackTable : queries
    FeedbackService ..> Feedback : creates
    FeedbackService ..> FeedbackResponse : returns
    Feedback --> FeedbackStatus
    FeedbackQuery --> FeedbackStatus
```

Nullable types (`Feedback?`, `String?`) are drawn without the `?`; the code shows which values may be `null`.

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

`PATCH /api/v1/feedback/{id}` with `{ "message": "..." }`, by the owner or by someone with `feedback.update`:

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant JWT as Authentication plugin
    participant Routes as FeedbackRoutes
    participant Service as FeedbackService
    participant Authz as AuthorizationService
    participant Repo as FeedbackRepository

    Client->>JWT: PATCH /feedback/{id} with Bearer token
    JWT->>JWT: check signature, issuer, audience, expiry
    JWT->>JWT: validate: user exists, is active, ver equals token version
    alt token missing or invalid
        JWT-->>Client: 401 UNAUTHORIZED
    end

    JWT->>Routes: authenticated call
    Routes->>Routes: uuidParameter("id")
    alt id is not a UUID
        Routes-->>Client: 400 INVALID_REQUEST
    end

    Routes->>Service: getOwnerId(id)
    Service->>Repo: findById(id)
    alt item not found
        Service-->>Client: 404 FEEDBACK_NOT_FOUND
    end
    Service-->>Routes: ownerId

    Routes->>Authz: requireOwnerOrPermission(currentUserId, ownerId, FEEDBACK_UPDATE)
    alt not the owner and no feedback.update
        Authz-->>Client: 403 FORBIDDEN
    end

    Routes->>Service: updateMessage(id, request)
    alt message empty or longer than 2000 characters
        Service-->>Client: 400 INVALID_REQUEST
    end
    Service->>Repo: findById(id), then updateMessage(id, message)
    Service-->>Routes: FeedbackResponse
    Routes-->>Client: 200 FeedbackResponse
```

- Every error is an `AppException`; `plugins/StatusPages.kt` turns it into `{ "code", "message" }`. The diagram
  draws the answer straight to the client to stay readable.
- The owner is read **before** the access check, so an unknown id gives 404, not 403. The service reads the item once
  more for the 404 and the response, then writes only the message column.
- The list (`GET /feedback`) is the one route that narrows instead of refusing: without `feedback.read`, the route
  sets `ownerId` to the caller, whatever filter was sent.

## Tests

In `backend/test/features/feedback/`:

| File | Kind | What it tests |
|---|---|---|
| `FeedbackServiceTest.kt` | unit, with the fake | validation, the starting status, both updates, delete, 404 |
| `FeedbackMapperTest.kt` | unit | `Feedback.toResponse()` |
| `FeedbackRepositoryTest.kt` | repository, against H2 | search, filters, a `%` in the text matched literally, sort, paging, each update changing only its own column, delete, the foreign key |
| `FeedbackRoutesTest.kt` | routes, test host | every endpoint with 200/201/204 and 400/401/403/404, and the list narrowing |
| `FakeFeedbackRepository.kt` | fake | keeps items in a map and behaves like the Exposed repository |

The numbers per run: `docs/test-report.md`.

## Copying it

The step-by-step version, for a feature without an owner: `docs/guide/08-tutorial-categories.md`.
