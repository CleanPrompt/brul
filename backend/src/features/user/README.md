# User

User accounts: the model, persistence and the user endpoints. The `authentication` feature reuses
`UserRepository` instead of having its own user storage.

Users are an exception to the reference pattern: they are created through `/auth/register`, deactivated instead
of deleted, and own their own account. To build a new feature, copy `feedback` instead.
