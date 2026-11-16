# Learning Guide

A step-by-step guide to understanding this project and building on it, for students who are new to Kotlin, Ktor and
Exposed. Read the chapters in order the first time; later, use them as a reference.

The project is a REST API that a future Android app will use. It has users, login with tokens, role-based access
control, a reference feature called `feedback`, and search and paging on every list. Everything is kept small and
explainable on purpose: you should be able to explain every line you write.

## How to use this guide

- **Follow along in the code.** Every chapter names the files it talks about. Open them next to the text. The
  `README.md` in each feature folder tells you what every file there does.
- **Run things.** Start the server, run the tests, send requests. You learn more from one failing request than from
  ten pages of text.
- **Using an AI assistant?** Give it [`llm-context.md`](llm-context.md) first. It tells the assistant the rules of
  this project, so the code it generates fits in. You still have to understand and check what it writes.
- **Test yourself.** Each chapter ends with a link to its questions in the [quiz](09-quiz.md).

## Chapters

| # | Chapter | You will learn |
|---|---|---|
| 1 | [Getting started](01-getting-started.md) | Install, configure, run the server and the tests, send your first requests |
| 2 | [Kotlin, Ktor and Exposed](02-kotlin-ktor-exposed.md) | The Kotlin this project uses, and the libraries: web server, database, JSON, build tool |
| 3 | [Architecture and the life of a request](03-architecture-and-requests.md) | Features, layers, the wiring in `Application.kt`, and one request from HTTP to the database and back |
| 4 | [Security](04-security.md) | Authentication (register, login, JWT, logout) and authorization (roles, permissions, owners, 401 versus 403) |
| 5 | [Errors and the database](05-errors-and-database.md) | `AppException`, `requireValid()`, one error format; tables, repositories, transactions, schema changes |
| 6 | [Lists](06-lists.md) | Search, filter, sort and paging with query parameters and `PageResponse` |
| 7 | [Testing](07-testing.md) | Fakes, route tests, repository tests |
| 8 | [Tutorial: add a `categories` feature](08-tutorial-categories.md) | Build a complete feature yourself, step by step |
| 9 | [Quiz](09-quiz.md) | Multiple-choice questions per chapter, with explained answers |
| 10 | [Glossary](10-glossary.md) | Short definitions of every term |
| – | [Context for AI assistants](llm-context.md) | Project rules and known pitfalls, written for an LLM |

Why the project is built this way, with the alternatives: the decisions tables in
[`docs/implementation-plan.md`](../implementation-plan.md#decisions). Where every other fact lives: the root
`README.md`.
