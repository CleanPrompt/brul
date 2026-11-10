# Vote

Community voting and score engine for feedback items.

What it shows:
- Binary votes (`LIKE` and `DISLIKE`).
- Public summary endpoint (`GET /api/v1/feedback/{id}/votes`) with total score and like/dislike counts.
- Authenticated voting (`PUT /api/v1/feedback/{id}/vote`) returning `201 Created` for a new vote or `200 OK` for an update.
- Anti-self-voting business rule: users cannot vote on their own feedback (`403 Forbidden`).
- Retrieving and deleting the authenticated user's vote (`GET` / `DELETE /api/v1/feedback/{id}/vote[/me]`).
- Composite unique index on `(feedback_id, user_id)` and cascade deletion when the feedback or user is deleted.
