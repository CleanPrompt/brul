package nl.brul.backend.features.authentication

import nl.brul.backend.TEST_ADMIN_EMAIL
import nl.brul.backend.TEST_PASSWORD
import nl.brul.backend.TestServices
import nl.brul.backend.features.authentication.dto.ChangePasswordRequest
import nl.brul.backend.features.authentication.dto.LoginRequest
import nl.brul.backend.features.authentication.dto.RegisterRequest
import nl.brul.backend.features.authorization.model.DefaultRole
import nl.brul.backend.shared.error.AppException
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthenticationServiceTest {
    private val services = TestServices()
    private val service = services.authenticationService

    private fun register(email: String = "alice@example.com") =
        service.register(RegisterRequest(email, "Alice", TEST_PASSWORD))

    private fun roleNamesOf(userId: String): Set<String> =
        services.roleService.findRolesForUser(UUID.fromString(userId)).map { it.name }.toSet()

    @Test
    fun `register normalizes email and stores a hashed password`() {
        val response = register(email = "  Alice@Example.COM ")

        assertEquals("alice@example.com", response.user.email)
        assertTrue(response.accessToken.isNotBlank())
        assertNotEquals(TEST_PASSWORD, services.userRepository.findByEmail("alice@example.com")?.passwordHash)
    }

    @Test
    fun `register sets createdAt to the current time in UTC`() {
        val before = Instant.now().minusSeconds(1)

        val createdAt = register().user.createdAt

        assertTrue(createdAt.endsWith("Z"), "createdAt should be UTC: $createdAt")
        assertTrue(Instant.parse(createdAt) in before..Instant.now())
    }

    @Test
    fun `register gives a new user the USER role`() {
        val response = register()

        assertEquals(setOf(DefaultRole.USER.name), roleNamesOf(response.user.id))
    }

    @Test
    fun `register gives the bootstrap admin the ADMIN role`() {
        val response = register(email = TEST_ADMIN_EMAIL)

        assertEquals(setOf(DefaultRole.USER.name, DefaultRole.ADMIN.name), roleNamesOf(response.user.id))
    }

    @Test
    fun `register rejects a duplicate email`() {
        register()

        val error = assertFailsWith<AppException.Conflict> { register() }
        assertEquals("EMAIL_ALREADY_REGISTERED", error.code)
    }

    @Test
    fun `register rejects an invalid email`() {
        assertFailsWith<AppException.BadRequest> { register(email = "not-an-email") }
    }

    @Test
    fun `register rejects a short password`() {
        assertFailsWith<AppException.BadRequest> {
            service.register(RegisterRequest("alice@example.com", "Alice", "short"))
        }
    }

    @Test
    fun `login succeeds with correct credentials`() {
        val registered = register()

        val response = service.login(LoginRequest("ALICE@example.com", TEST_PASSWORD))

        assertEquals(registered.user.id, response.user.id)
    }

    @Test
    fun `login rejects a wrong password`() {
        register()

        val error = assertFailsWith<AppException.Unauthorized> {
            service.login(LoginRequest("alice@example.com", "Wrong-password-123"))
        }
        assertEquals("INVALID_CREDENTIALS", error.code)
    }

    @Test
    fun `login rejects an unknown email with the same error`() {
        val error = assertFailsWith<AppException.Unauthorized> {
            service.login(LoginRequest("nobody@example.com", TEST_PASSWORD))
        }
        assertEquals("INVALID_CREDENTIALS", error.code)
    }

    @Test
    fun `change password replaces the password and raises the token version`() {
        val registered = register()
        val userId = UUID.fromString(registered.user.id)

        val response = service.changePassword(userId, ChangePasswordRequest(TEST_PASSWORD, NEW_PASSWORD))

        assertEquals(1, services.userRepository.findById(userId)?.tokenVersion)
        assertNotEquals(registered.accessToken, response.accessToken)
        assertEquals(registered.user.id, service.login(LoginRequest("alice@example.com", NEW_PASSWORD)).user.id)
        assertFailsWith<AppException.Unauthorized> {
            service.login(LoginRequest("alice@example.com", TEST_PASSWORD))
        }
    }

    @Test
    fun `change password rejects a wrong current password`() {
        val userId = UUID.fromString(register().user.id)

        val error = assertFailsWith<AppException.BadRequest> {
            service.changePassword(userId, ChangePasswordRequest("Wrong-password-123", NEW_PASSWORD))
        }
        assertEquals("INVALID_CURRENT_PASSWORD", error.code)
        assertEquals(0, services.userRepository.findById(userId)?.tokenVersion)
    }

    @Test
    fun `change password rejects a short new password`() {
        val userId = UUID.fromString(register().user.id)

        assertFailsWith<AppException.BadRequest> {
            service.changePassword(userId, ChangePasswordRequest(TEST_PASSWORD, "short"))
        }
    }

    @Test
    fun `change password rejects a new password equal to the current one`() {
        val userId = UUID.fromString(register().user.id)

        assertFailsWith<AppException.BadRequest> {
            service.changePassword(userId, ChangePasswordRequest(TEST_PASSWORD, TEST_PASSWORD))
        }
    }

    @Test
    fun `login rejects a deactivated user`() {
        register()
        val user = services.userRepository.findByEmail("alice@example.com")!!
        services.userRepository.update(user.copy(active = false))

        assertFailsWith<AppException.Unauthorized> {
            service.login(LoginRequest("alice@example.com", TEST_PASSWORD))
        }
    }

    private companion object {
        const val NEW_PASSWORD = "New-password-456"
    }
}
