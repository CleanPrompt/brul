# Report

Neighbourhood reports: something unsafe (`SAFETY`), something annoying (`NUISANCE`) or a request for help
(`HELP_REQUEST`), each with a location, plus up to three photos per report with the GPS position where the photo
was taken. Built on the pattern of `feedback` (owner, `requireOwnerOrPermission`, `getOwnerId`, hard delete).

What it adds to that pattern: domain rules in the model (the categories of each type, the extra field per type in
`ReportDetails`, the status that only moves forward), a search on distance (`ReportSearch`), a view count that the
database increments, and file uploads (`ReportPhotoService`, `PhotoStorage`, `PhotoGpsReader`).

Endpoints, permissions and error codes: `TECHNICAL_DESIGN.md`. Decisions: `docs/implementation-plan.md`. A Dutch
explanation of every file: `Uitleg-report-en-fotos.txt`. Manual requests: `backend/http/6-reports.http`.

## Changes outside this feature

Everything else lives in `features/report/` and `test/features/report/`. These files outside it were changed; every
change is an addition, nothing existing was removed or renamed.

| File | Change | Why |
|---|---|---|
| `shared/error/AppException.kt` | New subclasses `PayloadTooLarge` (413) and `UnsupportedMediaType` (415) | Photo over 5 MB, file that is not a JPEG or PNG |
| `plugins/StatusPages.kt` | Maps the two new exceptions to 413 and 415 | Same `{ code, message }` body as the other errors |
| `features/authorization/model/Permission.kt` | `REPORT_UPDATE`, `REPORT_DELETE`, `REPORT_PHOTO_DELETE` | Admin access to reports and photos of others; `ADMIN` gets them through `Permission.entries`, the seeder stores them |
| `features/authorization/model/DefaultRole.kt` | KDoc only: `USER` may access its own reports and photos | Keeps the comment true |
| `config/DatabaseFactory.kt` | `ReportsTable` and `ReportPhotosTable` added to `SchemaUtils.create` | New tables only, so an existing `data/brul*` database keeps working |
| `plugins/Routing.kt` | Parameters `reportService` and `reportPhotoService`; `reportRoutes` and `reportPhotoRoutes` inside `authenticate` | Registers the endpoints |
| `Application.kt` | Creates the repositories, `FileSystemPhotoStorage(Path.of("data", "uploads"))` and both services | Wiring |
| `test/TestFixtures.kt` | `TestServices` gets the fakes and both services; `configureTestApp` passes them on | Same wiring as `module()`, with fakes |
| `.gitignore` | `data/uploads/` | Uploaded photos are not committed |

Documentation outside this feature that now mentions reports: `TECHNICAL_DESIGN.md`, `docs/implementation-plan.md`
(Phase 14), `docs/diagrams/architecture.puml`, the root `README.md`, `backend/resources/openapi/openapi.yaml`,
`backend/http/README.md`.

## Where `docs/guide` no longer matches the code

`docs/guide` is left unchanged on purpose: it describes the base with `feedback` as the example. These parts of it are
now incomplete because of this feature:

| Chapter | What it shows | What is different now |
|---|---|---|
| `README.md` | "a reference feature called `feedback`" | There is a second feature with an owner: `report` |
| `04-architecture.md` | The folder tree, the `module()` wiring and the dependency diagram list `feedback` as the only feature | `report` sits next to it, with the same dependencies (`AuthorizationService`; `reports.owner_id` points to `users`) |
| `07-authorization.md` | The `Permission` enum ends at `FEEDBACK_DELETE` | Three report permissions follow it |
| `08-errors-and-validation.md` | `AppException` has five subclasses (400–409) | Seven: also 413 and 415 |
| `09-database.md` | `SchemaUtils.create(...)` and the diagram end at `FeedbackTable` | Also `ReportsTable` and `ReportPhotosTable` (FK to `reports`, `ON DELETE CASCADE`) |
| `10-lists-search-paging.md` | Every list is one SQL query with `search` and `count`, and every list returns a `PageResponse` | `GET /reports` does its distance part in Kotlin, and the photo list is a plain array (see below) |
| `11-testing.md` | `TestServices` with `feedbackService` as the last service | Also the report fakes and both report services |

## Deviations from the guide

- **Search on distance in Kotlin.** H2 has no distance function. `ExposedReportRepository.findMatching` applies the
  text and exact filters in SQL; `ReportSearch` then applies the area (`latitude`, `longitude`, `radiusKm` up to
  100), sorts on `createdAt`, `viewCount` or `distance` (ties on id, so pages stay stable) and cuts the page. The
  response is still a `PageResponse`, and `total` counts the matches inside the area. Fine for a neighbourhood app;
  with many thousands of reports this belongs in the database.
- **Photo list without paging.** `GET /reports/{id}/photos` returns a plain JSON array: there are never more than 3.
- **Status inside `PUT`.** There is no separate status endpoint: `PUT /reports/{id}` sends the whole report including
  `status`. A step back (for example `RESOLVED` to `OPEN`) gives 409 `INVALID_STATUS_CHANGE`.
- **The type of a report never changes.** A `PUT` with a different `type` gives 400.
- **A view on every `GET /reports/{id}`,** also by the owner. The database adds one (`view_count + 1`), so two readers
  at the same time are both counted.
- **Photo files on disk, not in H2.** `FileSystemPhotoStorage` writes to `data/uploads/` under `<photo id>.<jpg|png>`;
  the table stores that key. When the database insert fails, the file is removed again; when a report is deleted,
  `ReportService` removes its files (the rows go through the cascade).
- **Uploads read as multipart,** not JSON: field `photo` and optional `latitude` and `longitude`. The size is checked
  twice: first `Content-Length` (5 MB plus room for the multipart headers), then while reading, which stops after
  5 MB + 1 byte.
- **Adding a photo needs `report.update`** for someone else's report, the same permission as changing that report.
  Deleting a photo: the uploader, or `report.photo.delete`.
- **GPS stays in the stored file** and is part of the JSON. Every logged-in user can see where a photo was taken.

## Not built (yet)

- Sorting reports on likes: belongs to `vote`, which does not exist in this base yet.
- The check on the number of volunteers of a help request: belongs to `help`. `volunteersNeeded` is stored (1–50),
  nothing counts against it yet.
- Viewing reports and photos without logging in: in Brul2 that was public; here every route is inside `authenticate`.
- Removing the GPS from the stored file (privacy) and resizing photos.
