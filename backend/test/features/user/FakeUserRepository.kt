package nl.example.backend.features.user

import nl.example.backend.features.user.model.User
import nl.example.backend.features.user.model.UserQuery
import nl.example.backend.features.user.model.UserSortField
import nl.example.backend.features.user.repository.UserRepository
import nl.example.backend.shared.paging.SortDirection
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

    override fun update(user: User): Boolean {
        val existing = users[user.id] ?: return false
        // Like the real repository: update never changes the token version.
        users[user.id] = user.copy(tokenVersion = existing.tokenVersion)
        return true
    }

    override fun incrementTokenVersion(id: UUID) {
        val user = users[id] ?: return
        users[id] = user.copy(tokenVersion = user.tokenVersion + 1)
    }
}
