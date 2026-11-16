# Health

`GET /health` answers `OK` (200) when the server is running. It is public and lives outside `/api/v1`, so a
monitoring tool or a quick check in the browser needs no token.

## Files

| File | What it does |
|---|---|
| `route/HealthRoutes.kt` | The only route; it does not touch the database |

A feature only gets the folders it needs; this one needs just `route/`.
