# Feedback

The reference feature: CRUD for items owned by a user, with an admin override and search on the list. Copy it when
you add a resource that belongs to a user.

What it shows: the caller becomes the owner, `requireOwnerOrPermission` with `getOwnerId`, a status that only
`feedback.update` may change (on its own endpoint), a hard delete, a foreign key to `users`, and a list where the
route keeps a regular user within their own items.

How to copy a feature step by step: `docs/guide/12-tutorial-categories.md`. Endpoints and permissions:
`TECHNICAL_DESIGN.md`.
