package nl.brul.backend.features.user

import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.model.UserQuery
import nl.brul.backend.features.user.model.UserSortField
import nl.brul.backend.features.user.repository.UserRepository
import nl.brul.backend.shared.paging.SortDirection
import java.util.UUID

/** In-memory [UserRepository] so service and route tests do not need a database. */
class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<UUID, User>()

    override fun findById(id: UUID): User? = users[id]

    override fun findByEmail(email: String): User? = users.values.find { it.email == email }

    /** Same filters, order and page as the SQL query in `ExposedUserRepository`. */
    override fun search(query: UserQuery): List<User> {
        val sorted = when (query.sortField) {
            UserSortField.CREATED_AT -> matching(query).sortedBy { it.createdAt }
            UserSortField.DISPLAY_NAME -> matching(query).sortedBy { it.displayName }
            UserSortField.EMAIL -> matching(query).sortedBy { it.email }
        }
        val ordered = if (query.sortDirection == SortDirection.DESC) sorted.reversed() else sorted
        return ordered.drop(query.page.offset).take(query.page.limit)
    }

    override fun count(query: UserQuery): Long = matching(query).size.toLong()

    private fun matching(query: UserQuery): List<User> =
        users.values.filter { user ->
            (query.active == null || user.active == query.active) &&
                (query.text == null ||
                    user.email.contains(query.text, ignoreCase = true) ||
                    user.displayName.contains(query.text, ignoreCase = true))
        }

    override fun save(user: User): User = user.also { users[it.id] = it }

    // Like the real repository: each function changes only its own field.

    override fun updateDisplayName(id: UUID, displayName: String): Boolean {
        val user = users[id] ?: return false
        users[id] = user.copy(displayName = displayName)
        return true
    }

    override fun updatePasswordHash(id: UUID, passwordHash: String): Boolean {
        val user = users[id] ?: return false
        users[id] = user.copy(passwordHash = passwordHash)
        return true
    }

    override fun setActive(id: UUID, active: Boolean): Boolean {
        val user = users[id] ?: return false
        users[id] = user.copy(active = active)
        return true
    }

    override fun incrementTokenVersion(id: UUID) {
        val user = users[id] ?: return
        users[id] = user.copy(tokenVersion = user.tokenVersion + 1)
    }
}
