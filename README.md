# Brul Backend

A REST API in Kotlin with Ktor (a feature-first modular monolith, built with JetBrains Amper) that provides users,
authentication, role-based access control and a reference feature. It is the base for an Android app.

## Getting started

1. Copy `backend/.env.example` to `backend/.env` and set `JWT_SECRET` to a long random value.
2. Set `BOOTSTRAP_ADMIN_EMAIL`: the account that registers with this email becomes ADMIN.
3. Run the tests: `./kotlin test` (Windows: `kotlin.bat test`).
4. Start the server: `./kotlin run`, then open <http://127.0.0.1:8080/health>.
5. Try the API with the request files in `backend/http/` (see `backend/http/README.md`).

The H2 database is stored in `data/`. Delete `data/brul*` after changing an existing table. The full walkthrough,
with solutions for common problems, is `docs/guide/01-getting-started.md`.

New to the project, or to Kotlin, Ktor and Exposed? Read the learning guide: `docs/guide/README.md`. Using an AI
assistant? Give it `docs/guide/llm-context.md` first.

## Where to find what

Every fact is documented in exactly one place; other documents point to it. When you change one of these facts,
update its source.

| You want | Source |
|---|---|
| To learn how the project works and how to add a feature | `docs/guide/` |
| What the code of a feature or of `shared/` does | the `README.md` in that folder |
| The API contract: paths, parameters, request and response schemas | `backend/resources/openapi/openapi.yaml` |
| The endpoint overview with the required access | `TECHNICAL_DESIGN.md` §6 |
| The permission matrix | `DefaultRole.kt` in code; explained in `TECHNICAL_DESIGN.md` §4 |
| Error codes and status codes | `TECHNICAL_DESIGN.md` §5 |
| The data model | `TECHNICAL_DESIGN.md` §3 |
| Decisions, their alternatives and reasons; the history of the phases | `docs/implementation-plan.md` |
| The project rules for developers and AI assistants | `docs/guide/llm-context.md` (imported by `CLAUDE.md`) |
| Manual API checks | `backend/http/` and `backend/http/README.md` |

Each kind of document has one job: the guide **explains**, `TECHNICAL_DESIGN.md` **lists**, a feature README
**describes its code**. Explain a concept in one place and point to it from elsewhere; do not copy code into
documentation.

## Project structure

```text
.
├── README.md                 # this file: the entry point
├── TECHNICAL_DESIGN.md       # the reference: architecture, data model, security, errors, endpoints
├── CLAUDE.md                 # instructions for Claude Code; imports docs/guide/llm-context.md
├── project.yaml              # Amper project definition
├── kotlin, kotlin.bat        # Amper launcher scripts
├── docs/
│   ├── implementation-plan.md# decisions and the history of the phases
│   └── guide/                # learning guide, quiz, context file for AI assistants
└── backend/
    ├── module.yaml           # Amper module configuration and dependencies
    ├── .env.example          # configuration template (copy to .env, never commit .env)
    ├── http/                 # IntelliJ HTTP client requests; each one asserts its status code
    ├── resources/            # logback.xml, openapi/openapi.yaml
    ├── src/
    │   ├── Application.kt    # entry point; creates every object once and wires them together
    │   ├── config/           # AppConfig (settings from the environment), DatabaseFactory (connection, tables)
    │   ├── plugins/          # Ktor plugin configuration only, no business logic; one configure...() per file
    │   ├── shared/           # code used by more than one feature (README)
    │   └── features/         # one folder per feature, each with a README:
    │                         #   authentication, authorization, user, feedback (the reference), health
    └── test/                 # same structure as src/, plus TestFixtures.kt (TestServices, configureTestApp)
```
