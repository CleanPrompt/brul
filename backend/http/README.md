# HTTP requests

Manual API checks for the IntelliJ HTTP client. Every request asserts its expected status, so running a
whole file works as a smoke test; results appear in the Tests tab.

1. In `backend/.env`, set `BOOTSTRAP_ADMIN_EMAIL` to the `adminEmail` in `http-client.env.json`.
2. Start the server (`kotlin.bat run`) and choose the environment `dev`.
3. Run the files in order: `0-health`, `1-authentication`, `2-users`, `3-roles`, `4-feedback`, `6-help`, `5-logout`.
   Logout ends every session of the user, so run `1-authentication` again before re-running the others.
   `6-reports` needs valid tokens too: run it before `5-logout`, or run `1-authentication` again first. It uploads
   `report-photo.jpg`, a tiny photo with GPS data in it.

All files can be run repeatedly. The authentication endpoints allow 10 requests per minute; if you
re-run `1-authentication.http` quickly several times, wait a minute when you get 429.

Emails and the password live in `http-client.env.json`; tokens and ids are stored as global variables
by the response handlers.
