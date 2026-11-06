# Documentation

- `guide/` — learning guide for students: how the project works and how to build on it. Start at `guide/README.md`.
- `implementation-plan.md` — decisions with their reasons, phases (history) and what is out of scope.
- `diagrams/architecture.puml` — backend components.
- `../TECHNICAL_DESIGN.md` — the reference description of the system.
- `../backend/resources/openapi/openapi.yaml` — the API contract.

## One source per fact

Every fact is documented in exactly one place. Other documents point to it instead of copying it, so a change only
has to be made once. When you change one of these facts, update its source.

| Fact | Source |
|---|---|
| API contract: paths, parameters, request and response schemas | `backend/resources/openapi/openapi.yaml` |
| Endpoint overview with the required access | `TECHNICAL_DESIGN.md` §6 |
| Permission matrix (who may do what) | `DefaultRole.kt` in code; explained in `TECHNICAL_DESIGN.md` §4 |
| Error codes and status codes | `TECHNICAL_DESIGN.md` §5 |
| Data model | `TECHNICAL_DESIGN.md` §3 |
| Decisions and their reasons | `docs/implementation-plan.md` (decisions table) |
| History of the phases | `docs/implementation-plan.md` |
| Project rules for developers and AI assistants | `docs/guide/llm-context.md` (imported by `CLAUDE.md`) |
| How to learn the project and add a feature | `docs/guide/` (checklist in chapter 12) |
| Setup and running | `docs/guide/01-getting-started.md`; short version in `backend/README.md` |
| Manual API checks | `backend/http/` and `backend/http/README.md` |
| What a source folder is for | the short `README.md` in that folder |

The learning guide explains concepts with examples taken from these sources. When an example in the guide no longer
matches the code, fix the guide.
