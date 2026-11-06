package nl.brul.backend.features.user

import kotlinx.serialization.json.Json
import nl.brul.backend.features.user.mapper.toResponse
import nl.brul.backend.features.user.model.User
import nl.brul.backend.TEST_CREATED_AT
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UserMapperTest {
    private val user = User(UUID.randomUUID(), "a@example.com", "Alice", "secret-hash", true, TEST_CREATED_AT)

    @Test
    fun `maps all public fields`() {
        val response = user.toResponse()

        assertEquals(user.id.toString(), response.id)
        assertEquals("a@example.com", response.email)
        assertEquals("Alice", response.displayName)
        assertEquals(true, response.active)
        assertEquals("2026-01-01T12:00:00Z", response.createdAt)
    }

    @Test
    fun `serialized response contains no password hash`() {
        val json = Json.encodeToString(user.toResponse())

        assertFalse(json.contains("passwordHash"), "JSON must not contain a passwordHash field")
        assertFalse(json.contains("secret-hash"), "JSON must not contain the hash value")
    }
}
