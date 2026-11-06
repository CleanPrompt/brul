package nl.brul.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import nl.brul.backend.features.authentication.service.JwtService
import nl.brul.backend.features.user.repository.UserRepository
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.http.parseUuid
import java.util.UUID

/** Name of the JWT provider; use it as `authenticate(JWT_AUTHENTICATION) { ... }`. */
const val JWT_AUTHENTICATION = "jwt"

fun Application.configureAuthentication(
    jwtService: JwtService,
    userRepository: UserRepository
) {
    authentication {
        jwt(JWT_AUTHENTICATION) {
            realm = "brul-api"

            // Checks signature, issuer, audience and expiration.
            verifier(jwtService.verifier)

            // A valid token is only accepted while its user exists, is active and has the same token version.
            validate { credential ->
                val userId = credential.payload.subject
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@validate null

                // A token without a version claim is rejected (deny by default).
                val tokenVersion = credential.payload.getClaim(JwtService.TOKEN_VERSION_CLAIM).asInt()
                    ?: return@validate null

                val user = userRepository.findById(userId)

                if (user != null && user.active && user.tokenVersion == tokenVersion) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

/** The id of the authenticated user (`sub` claim). Only call inside `authenticate(JWT_AUTHENTICATION)`. */
fun ApplicationCall.currentUserId(): UUID {
    val principal = principal<JWTPrincipal>()
        ?: throw AppException.Unauthorized("Authentication required")
    return parseUuid(principal.subject, "token subject")
}
