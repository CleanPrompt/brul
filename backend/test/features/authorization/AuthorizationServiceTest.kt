package nl.example.backend.features.authorization

import nl.example.backend.features.authorization.model.DefaultRole
import nl.example.backend.features.authorization.model.Permission
import nl.example.backend.features.authorization.service.AuthorizationSeeder
import nl.example.backend.features.authorization.service.AuthorizationService
import nl.example.backend.features.authorization.service.RoleService
import nl.example.backend.features.user.FakeUserRepository
import nl.example.backend.features.user.model.User
import nl.example.backend.shared.error.AppException
import nl.example.backend.TEST_CREATED_AT
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthorizationServiceTest {
    private val userRepository = FakeUserRepository()
    private val authorizationRepository = FakeAuthorizationRepository().also { AuthorizationSeeder(it).seed() }
    private val roleService = RoleService(authorizationRepository, userRepository)
    private val service = AuthorizationService(authorizationRepository, userRepository)

    private fun newUser(vararg roles: DefaultRole, active: Boolean = true): UUID {
        val user = userRepository.save(
            User(UUID.randomUUID(), "${UUID.randomUUID()}@example.com", "Test", "hash", active, TEST_CREATED_AT)
        )
        roles.forEach { roleService.assignDefaultRole(user.id, it) }
        return user.id
    }

    @Test
    fun `admin has every permission`() {
        val admin = newUser(DefaultRole.USER, DefaultRole.ADMIN)

        Permission.entries.forEach { assertTrue(service.hasPermission(admin, it), "admin should have ${it.key}") }
    }

    @Test
    fun `regular user has no permissions`() {
        val user = newUser(DefaultRole.USER)

        Permission.entries.forEach { assertFalse(service.hasPermission(user, it), "user should not have ${it.key}") }
    }

    @Test
    fun `user without any role is denied`() {
        assertFalse(service.hasPermission(newUser(), Permission.USER_READ))
    }

    @Test
    fun `deactivated admin is denied`() {
        val admin = newUser(DefaultRole.ADMIN, active = false)

        assertFalse(service.hasPermission(admin, Permission.USER_READ))
    }

    @Test
    fun `revoking a role removes its permissions immediately`() {
        val admin = newUser(DefaultRole.ADMIN)
        val adminRole = authorizationRepository.findRoleByName(DefaultRole.ADMIN.name)!!

        val otherAdmin = newUser(DefaultRole.ADMIN)

        roleService.removeRole(admin, adminRole.id, currentUserId = otherAdmin)

        assertFalse(service.hasPermission(admin, Permission.USER_READ))
    }

    @Test
    fun `admins cannot remove their own ADMIN role`() {
        val admin = newUser(DefaultRole.USER, DefaultRole.ADMIN)
        val adminRole = authorizationRepository.findRoleByName(DefaultRole.ADMIN.name)!!

        assertFailsWith<AppException.BadRequest> { roleService.removeRole(admin, adminRole.id, currentUserId = admin) }
        assertTrue(service.hasPermission(admin, Permission.ROLE_ASSIGN))
    }

    @Test
    fun `admins can remove another role from their own account`() {
        val admin = newUser(DefaultRole.USER, DefaultRole.ADMIN)
        val userRole = authorizationRepository.findRoleByName(DefaultRole.USER.name)!!

        roleService.removeRole(admin, userRole.id, currentUserId = admin)

        assertTrue(service.hasPermission(admin, Permission.ROLE_ASSIGN))
    }

    @Test
    fun `users may access their own account without a permission`() {
        val user = newUser(DefaultRole.USER)

        service.requireOwnerOrPermission(user, user, Permission.USER_READ)
    }

    @Test
    fun `users may not access another account without the permission`() {
        val user = newUser(DefaultRole.USER)
        val other = newUser(DefaultRole.USER)

        assertFailsWith<AppException.Forbidden> {
            service.requireOwnerOrPermission(user, other, Permission.USER_READ)
        }
    }

    @Test
    fun `owners may access their own resource, others need the permission`() {
        val owner = newUser(DefaultRole.USER)
        val other = newUser(DefaultRole.USER)
        val admin = newUser(DefaultRole.ADMIN)

        service.requireOwnerOrPermission(owner, owner, Permission.FEEDBACK_DELETE)
        service.requireOwnerOrPermission(admin, owner, Permission.FEEDBACK_DELETE)
        assertFailsWith<AppException.Forbidden> {
            service.requireOwnerOrPermission(other, owner, Permission.FEEDBACK_DELETE)
        }
    }
}
