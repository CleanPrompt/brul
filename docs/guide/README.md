# Learning Guide

A step-by-step guide to understanding this project and building on it. It is written for students who are new to
Kotlin, Ktor and Exposed. Read the chapters in order the first time; later, use them as a reference.

The project is a REST API that a future Android app will use. It has users, login with tokens, role-based access
control, a reference feature called `feedback`, and search and paging on every list. Everything is kept small and
explainable on purpose: you should be able to explain every line you write.

## How to use this guide

- **Follow along in the code.** Every chapter names the files it talks about. Open them next to the text.
- **Run things.** Start the server, run the tests, send requests. You learn more from one failing request than from
  ten pages of text.
- **Using an AI assistant?** Give it [`llm-context.md`](llm-context.md) first. It tells the assistant the rules of
  this project, so the code it generates fits in. You still have to understand and check what it writes.
- **Test yourself.** Each part ends with a pointer to the [quiz](14-quiz.md).

## Chapters

### Part 1: Orientation

| # | Chapter | You will learn |
|---|---|---|
| 1 | [Getting started](01-getting-started.md) | Install, configure, run the server and the tests, send your first requests |
| 2 | [Kotlin primer](02-kotlin-primer.md) | The Kotlin features this project uses, with examples from the code |
| 3 | [Ktor, Exposed and friends](03-ktor-and-exposed-primer.md) | The libraries: web server, database access, JSON, build tool |

### Part 2: How it works

| # | Chapter | You will learn |
|---|---|---|
| 4 | [Architecture](04-architecture.md) | Features, layers, and how `Application.kt` wires everything together |
| 5 | [Life of a request](05-request-lifecycle.md) | One request followed from HTTP to the database and back |
| 6 | [Authentication](06-authentication.md) | Register, login, passwords, JWT tokens, logout |
| 7 | [Authorization](07-authorization.md) | Roles, permissions, owners, 401 versus 403 |
| 8 | [Errors and validation](08-errors-and-validation.md) | `AppException`, `requireValid()`, one error format |
| 9 | [Database](09-database.md) | Tables, mappers, transactions, schema changes |
| 10 | [Lists: search, filter, sort and paging](10-lists-search-paging.md) | Query parameters and `PageResponse` |
| 11 | [Testing](11-testing.md) | Fakes, route tests, repository tests |

### Part 3: Building on it

| # | Chapter | You will learn |
|---|---|---|
| 12 | [Tutorial: add a `categories` feature](12-tutorial-categories.md) | Build a complete feature yourself, step by step |
| 13 | [Design decisions](13-design-decisions.md) | Why things are the way they are, and the alternatives |

### Part 4: Check yourself

| # | Chapter | |
|---|---|---|
| 14 | [Quiz](14-quiz.md) | Multiple-choice questions per chapter, with explained answers |
| 15 | [Glossary](15-glossary.md) | Short definitions of every term |
| – | [Context for AI assistants](llm-context.md) | Project rules and known pitfalls, written for an LLM |

## Other documents

- `README.md` in the repository root: the folder structure.
- `TECHNICAL_DESIGN.md`: the reference description (data model, security, API table). This guide explains; the
  design document lists.
- `docs/implementation-plan.md`: the decisions table and the history of the phases.
- `backend/resources/openapi/openapi.yaml`: the exact API contract.
- Every folder in `backend/src/` has a short `README.md`.
