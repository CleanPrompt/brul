package nl.brul.backend.features.authentication.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import nl.brul.backend.config.AppConfig
import java.time.Instant
import java.util.Date
import java.util.UUID

class JwtService(
    config: AppConfig
) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)
    private val issuer = config.jwtIssuer
    private val audience = config.jwtAudience

    val expiresInSeconds: Long = 3600

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    /** [tokenVersion] goes into the `ver` claim; a token is only accepted while it matches the database. */
    fun generateToken(userId: UUID, tokenVersion: Int): String {
        val now = Instant.now()

        return JWT.create()
            .withSubject(userId.toString())
            .withClaim(TOKEN_VERSION_CLAIM, tokenVersion)
            .withIssuer(issuer)
            .withAudience(audience)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(expiresInSeconds)))
            .sign(algorithm)
    }

    companion object {
        const val TOKEN_VERSION_CLAIM = "ver"
    }
}