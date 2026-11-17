package nl.brul.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import nl.brul.backend.AppServices
import nl.brul.backend.features.authentication.route.authenticationRoutes
import nl.brul.backend.features.authentication.route.logoutRoutes
import nl.brul.backend.features.authentication.route.passwordRoutes
import nl.brul.backend.features.authorization.route.roleRoutes
import nl.brul.backend.features.feedback.route.feedbackRoutes
import nl.brul.backend.features.health.route.healthRoutes
import nl.brul.backend.features.user.route.profileImageRoutes
import nl.brul.backend.features.user.route.userRoutes

fun Application.configureRouting(services: AppServices) {
    routing {
        healthRoutes()

        route("/api/v1") {
            // Public, but rate limited.
            rateLimit(AUTHENTICATION_RATE_LIMIT) {
                route("/auth") {
                    authenticationRoutes(services.authenticationService)
                }
            }

            // Everything below requires a valid token; each route checks its own permission.
            authenticate(JWT_AUTHENTICATION) {
                route("/auth") {
                    logoutRoutes(services.authenticationService)
                }
                rateLimit(PASSWORD_CHANGE_RATE_LIMIT) {
                    passwordRoutes(services.authenticationService)
                }
                userRoutes(services.userService, services.authorizationService)
                profileImageRoutes(services.profileImageService, services.userService, services.authorizationService)
                roleRoutes(services.roleService, services.userService, services.authorizationService)
                feedbackRoutes(services.feedbackService, services.authorizationService)
            }
        }
    }
}
