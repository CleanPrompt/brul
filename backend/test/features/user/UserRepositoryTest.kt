package nl.example.backend.features.user

import nl.example.backend.features.user.database.UsersTable
import nl.example.backend.features.user.model.User
import nl.example.backend.features.user.model.UserQuery
import nl.example.backend.features.user.model.UserSortField
import nl.example.backend.features.user.repository.ExposedUserRepository
import nl.example.backend.shared.error.AppException
import nl.example.backend.shared.paging.Page
import nl.example.backend.shared.paging.SortDirection
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import nl.example.backend.TEST_CREATED_AT
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserRepositoryTest {
    private val repository = ExposedUserRepository()

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:user_repository_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(UsersTable) }
    }

    private fun newUser() =
        User(UUID.randomUUID(), "${UUID.randomUUID()}@example.com", "Alice", "hash", true, TEST_CREATED_AT)

    @Test
    fun `saves and retrieves a user`() {
        val user = repository.save(newUser())

        assertEquals(user, repository.findById(user.id))
        assertNotNull(repository.findByEmail(user.email))
    }

    // The in-memory database is shared by the tests in this class, so search tests use unique names.

    @Test
    fun `search matches part of the email or the display name, ignoring case`() {
        val marker = UUID.randomUUID().toString().take(8)
        val byEmail = repository.save(newUser().copy(email = "$marker@example.com"))
        val byName = repository.save(newUser().copy(displayName = "Alice $marker"))
        repository.save(newUser())

        val query = UserQuery(
            text = marker.uppercase(), sortField = UserSortField.EMAIL, sortDirection = SortDirection.ASC
        )

        assertEquals(setOf(byEmail, byName), repository.search(query).toSet())
        assertEquals(2, repository.count(query))
    }

    @Test
    fun `search filters on active and returns the requested page`() {
        val marker = UUID.randomUUID().toString().take(8)
        repeat(3) { repository.save(newUser().copy(displayName = "Inactive $marker", active = false)) }
        repository.save(newUser().copy(displayName = "Active $marker"))
        val query = UserQuery(text = marker, active = false, page = Page(limit = 2, offset = 0))

        assertEquals(2, repository.search(query).size)
        assertEquals(3, repository.count(query))
        assertTrue(repository.search(query).none { it.active })
    }

    @Test
    fun `updates an existing user`() {
        val user = repository.save(newUser())

        val updated = repository.update(user.copy(displayName = "Alicia", active = false))

        assertTrue(updated)
        assertEquals(user.copy(displayName = "Alicia", active = false), repository.findById(user.id))
    }

    @Test
    fun `incrementTokenVersion raises the version and update keeps it`() {
        val user = repository.save(newUser())

        repository.incrementTokenVersion(user.id)
        repository.update(user.copy(displayName = "Alicia"))

        assertEquals(1, repository.findById(user.id)?.tokenVersion)
    }

    @Test
    fun `rejects a second user with the same email`() {
        val user = repository.save(newUser())

        assertFailsWith<AppException.Conflict> {
            repository.save(newUser().copy(email = user.email))
        }
    }
}
