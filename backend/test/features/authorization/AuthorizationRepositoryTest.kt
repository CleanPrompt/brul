package nl.brul.backend.features.authorization

import nl.brul.backend.features.authorization.database.PermissionsTable
import nl.brul.backend.features.authorization.database.RolePermissionsTable
import nl.brul.backend.features.authorization.database.RolesTable
import nl.brul.backend.features.authorization.database.UserRolesTable
import nl.brul.backend.features.authorization.model.DefaultRole
import nl.brul.backend.features.authorization.model.Permission
import nl.brul.backend.features.authorization.repository.ExposedAuthorizationRepository
import nl.brul.backend.features.authorization.service.AuthorizationSeeder
import nl.brul.backend.features.user.database.UsersTable
import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.repository.ExposedUserRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import nl.brul.backend.TEST_CREATED_AT
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Runs the seeder and the RBAC queries against a real (in-memory) H2 database. */
class AuthorizationRepositoryTest {
    private val repository = ExposedAuthorizationRepository()
    private val userRepository = ExposedUserRepository()

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:authorization_repository_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(UsersTable, RolesTable, PermissionsTable, UserRolesTable, RolePermissionsTable)
        }
        AuthorizationSeeder(repository).seed()
    }

    private fun newUser(): UUID =
        userRepository.save(
            User(UUID.randomUUID(), "${UUID.randomUUID()}@example.com", "Test", "hash", true, TEST_CREATED_AT)
        ).id

    private fun role(defaultRole: DefaultRole) = repository.findRoleByName(defaultRole.name)!!

    @Test
    fun `seeding twice creates each default role once`() {
        AuthorizationSeeder(repository).seed()

        assertEquals(DefaultRole.entries.map { it.name }.sorted(), repository.findAllRoles().map { it.name }.sorted())
    }

    @Test
    fun `user gets the permissions of all assigned roles`() {
        val userId = newUser()
        repository.assignRoleToUser(userId, role(DefaultRole.USER).id)
        repository.assignRoleToUser(userId, role(DefaultRole.ADMIN).id)

        assertEquals(Permission.entries.toSet(), repository.findPermissionsForUser(userId))
    }

    @Test
    fun `removing a role removes its permissions`() {
        val userId = newUser()
        repository.assignRoleToUser(userId, role(DefaultRole.ADMIN).id)

        assertTrue(repository.removeRoleFromUser(userId, role(DefaultRole.ADMIN).id))
        assertEquals(emptySet(), repository.findPermissionsForUser(userId))
    }
}
