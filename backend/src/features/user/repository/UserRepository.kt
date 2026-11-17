package nl.brul.backend.features.user.repository

import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.model.UserQuery
import java.util.UUID

interface UserRepository {
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?

    /** One page of the users that match [query], in the requested order. */
    fun search(query: UserQuery): List<User>

    /** How many users match the filters of [query] in total, ignoring its page. */
    fun count(query: UserQuery): Long

    fun save(user: User): User

    // Each change writes only its own column, so two requests at the same time cannot undo each other's change.
    // Every function returns `false` when no user with [id] exists. The email never changes through the API.

    fun updateDisplayName(id: UUID, displayName: String): Boolean

    fun updatePasswordHash(id: UUID, passwordHash: String): Boolean

    fun setActive(id: UUID, active: Boolean): Boolean

    /** Invalidates every token issued to this user so far. */
    fun incrementTokenVersion(id: UUID)
}
