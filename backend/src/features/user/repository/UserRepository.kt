package nl.example.backend.features.user.repository

import nl.example.backend.features.user.model.User
import nl.example.backend.features.user.model.UserQuery
import java.util.UUID

interface UserRepository {
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?

    /** One page of the users that match [query], in the requested order. */
    fun search(query: UserQuery): List<User>

    /** How many users match the filters of [query] in total, ignoring its page. */
    fun count(query: UserQuery): Long

    fun save(user: User): User
    /** Updates the profile fields; never changes `tokenVersion`, see [incrementTokenVersion]. */
    fun update(user: User): Boolean

    /** Invalidates every token issued to this user so far. */
    fun incrementTokenVersion(id: UUID)
}
