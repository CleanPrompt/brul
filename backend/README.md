# Backend

Ktor backend module using JetBrains Amper and JVM 21.

1. Copy `.env.example` to `.env` and set `JWT_SECRET` to a long random value.
2. Set `BOOTSTRAP_ADMIN_EMAIL`: the account that registers with this email becomes ADMIN.
3. From the repository root, start the server with `./kotlin run` (Windows: `kotlin.bat run`) and run the tests
   with `./kotlin test`.
4. Try the API with the request files in `http/` (see `http/README.md`).

The H2 database is stored in `data/` in the repository root. Delete `data/brul*` after changing an existing table.
The full walkthrough is in `docs/guide/01-getting-started.md`.
