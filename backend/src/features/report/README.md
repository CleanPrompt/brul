# Report

Neighbourhood reports: something unsafe (`SAFETY`), something annoying (`NUISANCE`) or a request for help
(`HELP_REQUEST`), each with a location, plus up to three photos per report with the GPS position where the photo
was taken. Built on the pattern of `feedback` (owner, `requireOwnerOrPermission`, `getOwnerId`, hard delete).

What it adds to that pattern: domain rules in the model (the categories of each type, the extra field per type in
`ReportDetails`, the status that only moves forward), a search on distance (`ReportSearch`), a view count that the
database increments, and file uploads (`ReportPhotoService`, `PhotoStorage`, `PhotoGpsReader`).

Endpoints, permissions and error codes: `TECHNICAL_DESIGN.md`. Decisions: `docs/implementation-plan.md`.
