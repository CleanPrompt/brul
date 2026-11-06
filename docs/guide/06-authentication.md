# 6. Authentication

**Authentication** answers the question *who is the caller?* (Chapter 7 answers *what may the caller do?*: that is
authorization.) The code always writes both words in full, because "auth" could mean either. Only the URL prefix is
short: `/api/v1/auth`.

Files: `backend/src/features/authentication/` and `backend/src/plugins/Authentication.kt`.

## The endpoints

| Endpoint | What it does | Answer |
|---|---|---|
| `POST /api/v1/auth/register` | Create an account | 201 + token |
| `POST /api/v1/auth/login` | Log in | 200 + token |
| `POST /api/v1/auth/logout` | Log out on every device | 204 |
| `POST /api/v1/users/me/password` | Change your own password | 200 + new token |

Register and login are public but **rate limited**: at most 10 requests per minute (`plugins/RateLimiting.kt`).
That slows down someone who tries thousands of passwords. Password changes have their own limit of 5 per minute.

## Registering

`AuthenticationService.register`:

1. Normalise and check the input: the email is trimmed and lowercased; the display name must be 1–120 characters; the
   password 12–72 characters.
2. Refuse an email that is already registered: **409** `EMAIL_ALREADY_REGISTERED`.
3. **Hash** the password and store the user.
4. Give the user the `USER` role. If the email equals `BOOTSTRAP_ADMIN_EMAIL`, also give `ADMIN`.
5. Return a token, so the user is logged in immediately.

## Passwords: hashing with BCrypt

A password is never stored. The database holds a **hash**: the result of a one-way function. You cannot turn a hash
back into the password; you can only check whether a password produces the same hash.

```kotlin
// backend/src/features/authentication/service/PasswordHasher.kt
fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(12))
fun verify(password: String, passwordHash: String): Boolean = BCrypt.checkpw(password, passwordHash)
```

- **Salt**: `gensalt` adds random data, so two users with the same password get different hashes.
- **Cost 12**: BCrypt is deliberately slow (2^12 rounds). For one login that is fine; for an attacker who tries
  billions of passwords it is a wall.
- **Why at most 72 characters?** BCrypt ignores everything after 72 bytes. Rejecting longer passwords is more honest
  than silently cutting them.

## Logging in

```kotlin
fun login(request: LoginRequest): AuthenticationResponse {
    val email = request.email.trim().lowercase()
    val user = userRepository.findByEmail(email)

    if (user == null || !user.active || !passwordHasher.verify(request.password, user.passwordHash)) {
        throw AppException.Unauthorized("Invalid email or password", "INVALID_CREDENTIALS")
    }
    return createResponse(user)
}
```

Notice that an unknown email, a deactivated account and a wrong password all give **the same error**. If the API said
"unknown email", an attacker could find out who has an account.

## The token: a JWT

After a successful login the client receives:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI4YTFk...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "id": "8a1d...", "email": "test@example.com", ... }
}
```

The client sends the token with every later request: `Authorization: Bearer <accessToken>`.

A **JWT** (JSON Web Token) has three parts separated by dots: header, payload and signature. The payload is only
Base64-encoded, not encrypted: anyone can read it (try <https://jwt.io>). So it must never contain secrets. In this
project it contains only:

| Claim | Meaning |
|---|---|
| `sub` | Subject: the user id |
| `ver` | Token version (see logout below) |
| `iss` | Issuer: who created the token |
| `aud` | Audience: who the token is for |
| `iat` | Issued at |
| `exp` | Expires at (1 hour later) |

No email, no roles, no password hash.

The **signature** is what makes the token trustworthy. The server computes it with `HMAC256` and the `JWT_SECRET`.
If someone changes even one character of the payload (for example their `sub`), the signature no longer matches and
the token is rejected. Without the secret, nobody can create a valid signature.

```kotlin
// backend/src/features/authentication/service/JwtService.kt
JWT.create()
    .withSubject(userId.toString())
    .withClaim(TOKEN_VERSION_CLAIM, tokenVersion)
    .withIssuer(issuer)
    .withAudience(audience)
    .withIssuedAt(Date.from(now))
    .withExpiresAt(Date.from(now.plusSeconds(expiresInSeconds)))
    .sign(algorithm)
```

## Checking the token on every request

`plugins/Authentication.kt` installs the JWT plugin. For every route inside `authenticate(JWT_AUTHENTICATION)`:

1. The **verifier** checks signature, issuer, audience and expiry.
2. **`validate`** then checks the database: does the user still exist, are they active, and is the `ver` in the token
   the same as `token_version` in the database?

Only then does the handler run. `call.currentUserId()` returns the `sub` of that token.

## Logout: the token version

A JWT is valid until it expires; the server does not keep a list of issued tokens. So how do you log someone out?

Every user has a `token_version` column (start: 0). It is copied into each token as `ver`. Step 2 above rejects a
token whose `ver` differs from the database. To invalidate every token of a user, increase the number:

```kotlin
fun logout(userId: UUID) {
    userRepository.incrementTokenVersion(userId)   // UPDATE users SET token_version = token_version + 1
}
```

The same trick is used when an admin **deactivates** a user and when a user **changes their password**. Trade-off:
logout ends the sessions on *all* devices, not just one. For this project that is acceptable and much simpler than
storing every token.

## Changing the password

`POST /api/v1/users/me/password` with `currentPassword` and `newPassword`:

- The current password is required. A stolen token alone is not enough to take over the account.
- A wrong current password gives **400** `INVALID_CURRENT_PASSWORD`, not 401. The token itself is valid; a client
  treats 401 as "your session has expired, log in again", which would be confusing here.
- The token version goes up, so every other device is logged out. The response contains a new token, so the device
  that changed the password stays logged in.

## The first admin

There is no admin in an empty database, and no endpoint to create one (that would be a security hole). The setting
`BOOTSTRAP_ADMIN_EMAIL` solves this: whoever registers with that email gets the `ADMIN` role as well. No password is
stored in the code or in the configuration.

## Check yourself

[Quiz, chapter 6](14-quiz.md#chapter-6-authentication). Next: [chapter 7, authorization](07-authorization.md).
