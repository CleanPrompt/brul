package nl.brul.backend.features.user

import nl.brul.backend.features.user.dto.UpdateUserRequest
import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.service.UserService
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.TEST_CREATED_AT
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class UserServiceTest {
    private val repository = FakeUserRepository()
    private val service = UserService(repository)
    private val user = repository.save(User(UUID.randomUUID(), "a@example.com", "Alice", "hash", true, TEST_CREATED_AT))

    @Test
    fun `updates display name and preserves password hash`() {
        val response = service.updateDisplayName(user.id, UpdateUserRequest("  Alicia  "))

        assertEquals("Alicia", response.displayName)
        assertEquals("hash", repository.findById(user.id)?.passwordHash)
    }

    @Test
    fun `rejects empty display name`() {
        assertFailsWith<AppException.BadRequest> {
            service.updateDisplayName(user.id, UpdateUserRequest("   "))
        }
    }

    @Test
    fun `missing user gives 404`() {
        val error = assertFailsWith<AppException.NotFound> { service.getById(UUID.randomUUID()) }
        assertEquals("USER_NOT_FOUND", error.code)
    }

    @Test
    fun `deactivate marks another user inactive`() {
        service.deactivate(user.id, currentUserId = UUID.randomUUID())

        assertFalse(repository.findById(user.id)!!.active)
    }

    @Test
    fun `deactivate raises the token version`() {
        service.deactivate(user.id, currentUserId = UUID.randomUUID())

        assertEquals(1, repository.findById(user.id)?.tokenVersion)
    }

    @Test
    fun `reactivate marks a deactivated user active and keeps the token version`() {
        service.deactivate(user.id, currentUserId = UUID.randomUUID())

        service.reactivate(user.id)

        val reactivated = repository.findById(user.id)
        assertEquals(true, reactivated?.active)
        assertEquals(1, reactivated?.tokenVersion)
    }

    @Test
    fun `reactivate of a missing user gives 404`() {
        assertFailsWith<AppException.NotFound> { service.reactivate(UUID.randomUUID()) }
    }

    @Test
    fun `users cannot deactivate their own account`() {
        assertFailsWith<AppException.BadRequest> {
            service.deactivate(user.id, currentUserId = user.id)
        }
    }
}
