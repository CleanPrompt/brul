package nl.example.backend.features.authentication

import nl.example.backend.features.authentication.service.PasswordHasher
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordHasherTest {

    private val passwordHasher = PasswordHasher()

    @Test
    fun `hashed password can be verified`() {
        val password = "A-strong-test-password-123"

        val hash = passwordHasher.hash(password)

        assertTrue(passwordHasher.verify(password, hash))
    }

    @Test
    fun `incorrect password is rejected`() {
        val hash = passwordHasher.hash("Correct-password-123")

        assertFalse(
            passwordHasher.verify("Incorrect-password-123", hash)
        )
    }

    @Test
    fun `password hashes use different salts`() {
        val password = "A-strong-test-password-123"

        val firstHash = passwordHasher.hash(password)
        val secondHash = passwordHasher.hash(password)

        assertTrue(firstHash != secondHash)
    }
}