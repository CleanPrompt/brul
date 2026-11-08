# Brul Backend

A REST API in Kotlin with Ktor (a feature-first modular monolith, built with JetBrains Amper) that provides users,
authentication, role-based access control and a reference feature, as the base for an Android app.

## Getting started

1. Copy `backend/.env.example` to `backend/.env` and set `JWT_SECRET`.
2. Run the tests: `./kotlin test` (Windows: `kotlin.bat test`).
3. Start the server: `./kotlin run`, then open <http://127.0.0.1:8080/health>.

New to the project, or to Kotlin, Ktor and Exposed? Start with the learning guide: `docs/guide/README.md`. Using an AI
assistant? Give it `docs/guide/llm-context.md` first.

## Where to find what

| You want | Read |
|---|---|
| To learn the project and add a feature | `docs/guide/` |
| The reference: architecture, data model, security, errors, endpoints | `TECHNICAL_DESIGN.md` |
| The exact API contract | `backend/resources/openapi/openapi.yaml` |
| Decisions, their reasons and the history | `docs/implementation-plan.md` |
| Where each documented fact lives | `docs/README.md` |

## Project structure

```text
.
├── README.md
├── TECHNICAL_DESIGN.md
├── CLAUDE.md                 # instructions for Claude Code; imports docs/guide/llm-context.md
├── project.yaml              # Amper project definition
├── kotlin, kotlin.bat        # Amper launcher scripts
├── docs/
│   ├── guide/                # learning guide, quiz, context file for AI assistants
│   ├── implementation-plan.md
│   └── diagrams/
└── backend/
    ├── module.yaml           # Amper module configuration and dependencies
    ├── .env.example          # configuration template (copy to .env, never commit .env)
    ├── http/                 # IntelliJ HTTP client requests; each one asserts its status code
    ├── resources/            # logback.xml, openapi/openapi.yaml
    ├── src/
    │   ├── Application.kt    # entry point; wires all objects together
    │   ├── config/
    │   ├── plugins/
    │   ├── shared/           # error, http, paging, database helpers
    │   └── features/         # authentication, authorization, user, feedback, report, health
    └── test/                 # same structure as src/, plus TestFixtures.kt
```

Each source folder has a short `README.md` describing what it is for.
