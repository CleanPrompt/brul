# Authorization

Answers "what may the caller do?" with role-based access control, deny by default.

- `Permission` and `DefaultRole` (the permission matrix) are enums; `AuthorizationSeeder` stores them at startup.
- `AuthorizationService` holds every access rule; routes call `requirePermission(...)` or
  `requireOwnerOrPermission(...)`. Permissions are read from the database on every request.
- `RoleService` assigns and removes roles; roles are never created through the API.

Explained in `docs/guide/07-authorization.md`; the matrix is in `TECHNICAL_DESIGN.md` §4.
