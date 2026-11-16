# Shared

Code that more than one feature uses. It never depends on a feature: if only one feature needs something, it belongs
in that feature.

## Files

| File | What it does |
|---|---|
| `error/AppException.kt` | The expected errors as a sealed class: `BadRequest` (400), `Unauthorized` (401), `Forbidden` (403), `NotFound` (404), `Conflict` (409). Also `requireValid(condition) { message }`, which throws a 400; use it instead of Kotlin's `require()` |
| `error/ErrorResponse.kt` | The error body every failure returns: `{ "code": ..., "message": ... }` |
| `http/RequestParameters.kt` | `call.uuidParameter("id")` reads a path parameter as a UUID; an invalid one gives 400 |
| `http/QueryParameters.kt` | Readers for query parameters: `searchText`, `oneOf`, `booleanOrNull`, `uuidOrNull`, `sortDirection`, `page`. Each returns `null` or a default when the parameter is missing, and throws 400 with the parameter name when it is invalid |
| `paging/Page.kt` | `limit` (1–100, default 20) and `offset` of one page |
| `paging/SortDirection.kt` | `ASC` or `DESC` |
| `paging/PageResponse.kt` | The response of every list: `items`, `total`, `limit`, `offset` |
| `image/ImageType.kt` | Checks uploaded images for every feature that stores them. `ImageType.detect(bytes)` recognises a JPEG or PNG by its first bytes (never by the header or file name); `requireValidImage(bytes)` throws 400 `IMAGE_TOO_LARGE` above 2 MB or `INVALID_IMAGE_TYPE` for anything else. Each feature stores its own images |
| `database/QueryConditions.kt` | Exposed helpers for repositories: `containsIgnoringCase(text)` (a `LIKE` search where `%` and `_` match literally) and `SortDirection.toSortOrder()` |

## How a feature uses it

A list request goes through every folder here: the feature's query mapper reads the parameters with
`http/QueryParameters.kt` (400 on a bad value), the repository filters with `database/QueryConditions.kt`, and the
service returns a `paging/PageResponse`. Any `AppException` thrown on the way becomes an `ErrorResponse` in
`plugins/StatusPages.kt`.

Explained step by step: `docs/guide/06-lists.md` (lists) and `docs/guide/05-errors-and-database.md` (errors).
