# User

User accounts: the `User` model, its storage and the `/users` endpoints. The `authentication` feature reuses
`UserRepository` to register and log in, so there is only one place where users are stored.

Users differ from the reference feature (`feedback`), so do not copy this one:

- an account is created through `/auth/register` (in `authentication`), not through `/users`;
- an account is **deactivated**, never deleted (`active = false`), and can be reactivated;
- users **own their account**: the owner of `/users/{id}` is the account itself.

Endpoints and permissions: `TECHNICAL_DESIGN.md` §4 and §6.

## Files

| File | What it does |
|---|---|
| `route/UserRoutes.kt` | `GET /users/me`, the list, `GET`/`PATCH /users/{id}`, deactivate (`DELETE`) and reactivate. Each route does one access check before the work |
| `service/UserService.kt` | The rules: the display name is 1–120 characters; you cannot deactivate yourself; deactivating raises the token version so old tokens stop working. `getOwnerId(id)` returns the account id, or 404 |
| `repository/UserRepository.kt` | The interface: find by id or email, search, count, save, update, `incrementTokenVersion` |
| `repository/ExposedUserRepository.kt` | The Exposed implementation. `save` turns a duplicate email into 409; `incrementTokenVersion` is one atomic SQL update |
| `database/UsersTable.kt` | The `users` table, with a unique index on `email` |
| `model/User.kt` | The domain object, including `passwordHash` and `tokenVersion` (never sent to the client) |
| `model/UserQuery.kt`, `model/UserSortField.kt` | What a list request asks for, and the fields a client may sort on |
| `dto/UserResponse.kt` | The JSON the client receives: no password hash, no token version |
| `dto/UpdateUserRequest.kt` | JSON body of `PATCH /users/{id}`: `{ "displayName": ... }` |
| `mapper/UserMapper.kt` | `User.toResponse()`: model → JSON |
| `mapper/UserDatabaseMapper.kt` | `ResultRow.toUser()`: database row → model |
| `mapper/UserQueryMapper.kt` | `Parameters.toUserQuery()`: query parameters → `UserQuery`; an invalid value gives 400 |

## How a request flows

`PATCH /api/v1/users/{id}` with `{ "displayName": "..." }`:

1. The token is checked in `plugins/Authentication.kt` (401 without a valid one).
2. `UserRoutes` reads the id and asks `userService.getOwnerId(id)` (404 for an unknown user).
3. `requireOwnerOrPermission(currentUserId, ownerId, Permission.USER_UPDATE)`: your own account passes, someone
   else's needs `user.update` (403 otherwise).
4. `userService.updateDisplayName` trims and checks the name, then `repository.update` stores it.
5. The route responds with `UserResponse` (200).

Changing the password is in `authentication`, because hashing belongs there, even though its URL is
`/users/me/password`.
