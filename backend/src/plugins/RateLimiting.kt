package nl.brul.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import kotlin.time.Duration.Companion.minutes

val AUTHENTICATION_RATE_LIMIT = RateLimitName("authentication")

/** Separate from login, so guessing the current password with a stolen token is slowed down as well. */
val PASSWORD_CHANGE_RATE_LIMIT = RateLimitName("password-change")

fun Application.configureRateLimiting() {
    install(RateLimit) {
        register(AUTHENTICATION_RATE_LIMIT) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
        }
        register(PASSWORD_CHANGE_RATE_LIMIT) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
        }
    }
}