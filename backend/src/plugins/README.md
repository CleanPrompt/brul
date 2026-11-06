# Plugins

Ktor plugin configuration only; no business logic. `Application.module()` calls each `configure...()` function
once: serialization, status pages (errors), call logging, rate limiting, JWT authentication and routing.
Protected routes are registered inside `authenticate(JWT_AUTHENTICATION)` in `Routing.kt`.
