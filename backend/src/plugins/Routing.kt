package nl.example.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import nl.example.backend.features.authentication.route.authenticationRoutes
import nl.example.backend.features.authentication.route.logoutRoutes
import nl.example.backend.features.authentication.route.passwordRoutes
import nl.example.backend.features.authentication.service.AuthenticationService
import nl.example.backend.features.authorization.route.roleRoutes
import nl.example.backend.features.authorization.service.AuthorizationService
import nl.example.backend.features.authorization.service.RoleService
import nl.example.backend.features.feedback.route.feedbackRoutes
import nl.example.backend.features.feedback.service.FeedbackService
import nl.example.backend.features.health.route.healthRoutes
import nl.example.backend.features.user.route.userRoutes
import nl.example.backend.features.user.service.UserService

fun Application.configureRouting(
    authenticationService: AuthenticationService,
    userService: UserService,
    roleService: RoleService,
    authorizationService: AuthorizationService,
    feedbackService: FeedbackService
) {
    routing {
        healthRoutes()

        route("/api/v1") {
            // Public, but rate limited.
            rateLimit(AUTHENTICATION_RATE_LIMIT) {
                route("/auth") {
                    authenticationRoutes(authenticationService)
                }
            }

            // Everything below requires a valid token; each route checks its own permission.
            authenticate(JWT_AUTHENTICATION) {
                route("/auth") {
                    logoutRoutes(authenticationService)
                }
                rateLimit(PASSWORD_CHANGE_RATE_LIMIT) {
                    passwordRoutes(authenticationService)
                }
                userRoutes(userService, authorizationService)
                roleRoutes(roleService, authorizationService)
                feedbackRoutes(feedbackService, authorizationService)
            }
        }
    }
}
