# 1. Getting started

In this chapter you get the server running on your own machine, run the tests and send your first requests.

## What you need

- **IntelliJ IDEA** (Community or Ultimate). The HTTP request files in `backend/http/` run in IntelliJ's built-in
  HTTP client.
- **Git**, to clone the repository.
- An internet connection the first time: the build tool downloads itself and all libraries.

You do not need to install Kotlin, Gradle or Maven. The project uses **JetBrains Amper** as its build tool. The
scripts `kotlin` (macOS/Linux) and `kotlin.bat` (Windows) in the repository root download the right toolchain on
the first run.

## Step 1: configure

The server reads its settings from environment variables. For local development you put them in a file called
`.env` in the `backend/` folder.

1. Copy `backend/.env.example` to `backend/.env`.
2. Set `JWT_SECRET` to a long random value (at least 32 characters). It signs the login tokens; anyone who knows it
   can create valid tokens, so never commit `.env`.
3. Keep `BOOTSTRAP_ADMIN_EMAIL=admin@example.com`, or change it. The account that registers with this email
   becomes administrator. That is how the first admin is created on an empty database.

```text
HOST=127.0.0.1
PORT=8080
DATABASE_URL=jdbc:h2:file:./data/brul
JWT_SECRET=replace-with-a-long-random-secret
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
```

`AppConfig.load()` (`backend/src/config/AppConfig.kt`) reads these values. A real environment variable wins over the
`.env` file, which is how a server in production would be configured.

## Step 2: run the tests

From the repository root:

```text
./kotlin test        (macOS/Linux)
kotlin.bat test      (Windows)
```

The first run takes a few minutes because everything is downloaded. At the end you see a summary with the number of
`tests successful` and `0 tests failed`. The tests do not need a running server or the `.env` file.

> You will see some `WARN` lines and a stack trace during the test run. Some tests deliberately cause errors
> (a duplicate email, an unexpected exception) to check that the API handles them. Only the summary counts.

## Step 3: start the server

```text
./kotlin run         (macOS/Linux)
kotlin.bat run       (Windows)
```

When you see a line like `Responding at http://127.0.0.1:8080`, the server is running. Open
<http://127.0.0.1:8080/health> in a browser: it answers `OK`.

The database is a file: `data/brul.mv.db` in the repository root. It is created on the first start, together
with the roles and permissions.

## Step 4: send requests

Open the folder `backend/http/` in IntelliJ. Each `.http` file contains requests with a small test script that checks
the status code.

1. In the top bar of the editor, select the environment **`dev`**. Without it, `{{baseUrl}}` is not filled in.
2. Run the files in this order: `0-health`, `1-authentication`, `2-users`, `3-roles`, `4-feedback`, `5-logout`.
3. The results appear in the **Tests** tab.

`1-authentication.http` registers a normal user and the admin, logs in and stores the tokens in variables. The
other files use those tokens. `5-logout.http` logs the user out on every device, so run `1-authentication.http`
again before running the others a second time.

> **429 Too Many Requests?** Login and registration allow 10 requests per minute. If you run
> `1-authentication.http` several times quickly, wait a minute. That is the rate limiter doing its job.

## Step 5: look around

Try these yourself (replace the token with the `accessToken` from a login response):

```http
GET http://127.0.0.1:8080/api/v1/users/me
Authorization: Bearer <accessToken>
```

```http
GET http://127.0.0.1:8080/api/v1/feedback?q=slow&sort=createdAt&order=desc&limit=5
Authorization: Bearer <accessToken>
```

Send a request without the `Authorization` header and look at the answer:

```json
{ "code": "UNAUTHORIZED", "message": "Authentication required" }
```

Every error in this API has this shape. Chapter 8 explains why.

## When something goes wrong

| Problem | Cause and fix |
|---|---|
| `Missing required configuration: JWT_SECRET` | `backend/.env` is missing or has no `JWT_SECRET` |
| `{{baseUrl}}` appears literally in the request | Select the environment `dev` in the HTTP client |
| Strange database errors after you changed a table | Stop the server and delete `data/brul*`; see chapter 9 |
| `429` in the `.http` files | Wait a minute; see the rate limit above |
| The admin is not an admin | You registered the admin email before setting `BOOTSTRAP_ADMIN_EMAIL`. Delete `data/brul*` and register again |

## Where to go next

Before you read the code, learn the Kotlin features it uses: [chapter 2](02-kotlin-primer.md).
