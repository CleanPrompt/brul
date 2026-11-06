package nl.example.backend.features.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import nl.example.backend.features.authentication.service.JwtService
import nl.example.backend.testConfig
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JwtServiceTest {
    private val config = testConfig()
    private val jwtService = JwtService(config)

    @Test
    fun `token contains user id as subject and passes verification`() {
        val userId = UUID.randomUUID()

        val decoded = jwtService.verifier.verify(jwtService.generateToken(userId, tokenVersion = 3))

        assertEquals(userId.toString(), decoded.subject)
        assertEquals("test-issuer", decoded.issuer)
        assertEquals(listOf("test-audience"), decoded.audience)
        assertEquals(3, decoded.getClaim(JwtService.TOKEN_VERSION_CLAIM).asInt())
    }

    @Test
    fun `token signed with another secret is rejected`() {
        val otherService = JwtService(config.copy(jwtSecret = "a-different-secret"))
        val token = otherService.generateToken(UUID.randomUUID(), 0)

        assertFailsWith<JWTVerificationException> { jwtService.verifier.verify(token) }
    }

    @Test
    fun `token from another issuer is rejected`() {
        val otherService = JwtService(config.copy(jwtIssuer = "someone-else"))
        val token = otherService.generateToken(UUID.randomUUID(), 0)

        assertFailsWith<JWTVerificationException> { jwtService.verifier.verify(token) }
    }

    @Test
    fun `expired token is rejected`() {
        // Correctly signed, but expired one minute ago.
        val token = JWT.create()
            .withSubject(UUID.randomUUID().toString())
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withExpiresAt(Date.from(Instant.now().minusSeconds(60)))
            .sign(Algorithm.HMAC256(config.jwtSecret))

        assertFailsWith<TokenExpiredException> { jwtService.verifier.verify(token) }
    }
}
