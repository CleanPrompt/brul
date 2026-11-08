# Help & Volunteer Participation

Volunteer registration, capacity tracking, QR/OTP check-in with geofencing, and RFC 5545 `.ics` calendar export.

## What it shows:
- **Decoupled Architecture**: Depends on `HelpRequestProvider` (anti-corruption interface) rather than hard-coupling to other domain tables.
- **Pure Value Objects & Logic**: `CheckInValidator` with Haversine formula (150m geofence), deterministic status transitions, and `CalendarFeedService` generating RFC 5545 iCalendar data.
- **Concurrency & Storage Safety**: `HelpParticipationsTable` with a database unique index (`help_request_id`, `user_id`) to prevent race condition over-subscribing.
- **Public & Protected Endpoints**: Public `.ics` feed and capacity summary, alongside authenticated join, leave, owner OTP code generation, and volunteer check-in.

Detailed architectural documentation: `docs/help/`. Endpoints and schemas: `backend/resources/openapi/openapi.yaml`.
IntelliJ HTTP Client tests: `backend/http/6-help.http`.
