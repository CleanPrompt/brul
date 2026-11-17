package nl.brul.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import kotlin.time.Duration.Companion.minutes

val AUTHENTICATION_RATE_LIMIT = RateLimitName("authentication")

/** Separate from login, so guessing the current password with a stolen token is slowed down as well. */
val PASSWORD_CHANGE_RATE_LIMIT = RateLimitName("password-change")

/**
 * Every limiter needs a `requestKey`: each key gets its own bucket. Without one, Ktor gives every request the same
 * key, so one client that uses up the limit blocks everybody.
 *
 * Behind a proxy, `remoteHost` is the address of the proxy, so all clients would share one bucket again. This API
 * runs without a proxy; see `docs/guide/04-security.md`.
 */
fun Application.configureRateLimiting() {
    install(RateLimit) {
        // Login and registration have no token yet, so the key is the client's address.
        register(AUTHENTICATION_RATE_LIMIT) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
        // Registered inside `authenticate`, so the token has been checked: the key is the user id. Users on the same
        // network then do not block each other. The address is only a fallback if the route is ever moved.
        register(PASSWORD_CHANGE_RATE_LIMIT) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
            requestKey { call -> call.principal<JWTPrincipal>()?.subject ?: call.request.origin.remoteHost }
        }
    }
}
