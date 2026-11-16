# Authentication

Answers *who is the caller?*: registering, logging in, logging out and changing the password. It creates the tokens;
checking an incoming token on every request happens in `plugins/Authentication.kt`. What the caller may do is decided
by the `authorization` feature.

This feature has no table of its own: it stores users through `UserRepository` from the `user` feature. Concepts
(BCrypt, JWT, token version): `docs/guide/04-security.md`. Endpoints: `TECHNICAL_DESIGN.md` §6.

## Files

| File | What it does |
|---|---|
| `route/AuthenticationRoutes.kt` | Three route groups: `authenticationRoutes` (register, login; public and rate limited), `logoutRoutes` and `passwordRoutes` (both need a token). `plugins/Routing.kt` places each group |
| `service/AuthenticationService.kt` | The rules. `register` checks email, display name and password, refuses a known email (409), hashes the password, gives the role USER (and ADMIN for `BOOTSTRAP_ADMIN_EMAIL`) and returns a token. `login` gives the same error for every failure. `logout` and `changePassword` raise the token version |
| `service/PasswordHasher.kt` | `hash` and `verify` with BCrypt, cost 12 |
| `service/JwtService.kt` | `generateToken(userId, tokenVersion)` signs a JWT with `sub`, `ver`, `iss`, `aud`, `iat`, `exp`; `verifier` is used by the plugin to check tokens |
| `dto/RegisterRequest.kt`, `dto/LoginRequest.kt`, `dto/ChangePasswordRequest.kt` | The JSON bodies the client sends |
| `dto/AuthenticationResponse.kt` | The answer after register, login or password change: the token, its type and lifetime, and the user |
| `mapper/AuthenticationMapper.kt` | `User.toAuthenticationResponse(token, expiresIn)` |

## How a request flows

`POST /api/v1/auth/login` with `{ "email": ..., "password": ... }`:

1. The rate limiter in `plugins/RateLimiting.kt` allows 10 requests per minute (then 429).
2. `authenticationRoutes` reads the body and calls `authenticationService.login`.
3. The service lowercases the email, finds the user and checks with `PasswordHasher.verify`. Unknown email,
   inactive user and wrong password all give 401 `INVALID_CREDENTIALS`.
4. `JwtService.generateToken` creates a token with the user's current token version.
5. The route responds with `AuthenticationResponse` (200).

On every later request, `plugins/Authentication.kt` checks the signature and expiry, and that the user still exists,
is active and has the same token version as the `ver` in the token.
