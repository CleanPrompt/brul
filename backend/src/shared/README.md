# Shared

Code used by more than one feature; it never depends on a feature.

- `error/`: `AppException`, `requireValid()` and the `ErrorResponse` body.
- `http/`: reading path parameters (`uuidParameter`) and query parameters (`QueryParameters.kt`); invalid values
  give 400.
- `paging/`: `Page`, `SortDirection` and `PageResponse<T>`, the response of every list.
- `database/`: Exposed helpers for repositories: `containsIgnoringCase` and `SortDirection.toSortOrder()`.

How lists use these pieces: `docs/guide/10-lists-search-paging.md`.
