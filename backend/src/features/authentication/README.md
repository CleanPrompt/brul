# Authentication

Answers "who is the caller?": registration, login, logout, password changes, password hashing (`PasswordHasher`)
and JWT issuance (`JwtService`). Incoming tokens are checked in `plugins/Authentication.kt`. What a caller may do is
decided by the `authorization` feature.

The password route lives here because hashing belongs to authentication, even though its URL is
`/users/me/password`. Explained in `docs/guide/06-authentication.md`.
