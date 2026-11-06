package nl.brul.backend.features.authentication.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import nl.brul.backend.features.authentication.dto.ChangePasswordRequest
import nl.brul.backend.features.authentication.dto.LoginRequest
import nl.brul.backend.features.authentication.dto.RegisterRequest
import nl.brul.backend.features.authentication.service.AuthenticationService
import nl.brul.backend.plugins.currentUserId

/** Public and rate limited. */
fun Route.authenticationRoutes(authenticationService: AuthenticationService) {
    post("/register") {
        val request = call.receive<RegisterRequest>()
        call.respond(HttpStatusCode.Created, authenticationService.register(request))
    }

    post("/login") {
        val request = call.receive<LoginRequest>()
        call.respond(HttpStatusCode.OK, authenticationService.login(request))
    }
}

/** Register inside `authenticate(JWT_AUTHENTICATION)`; every logged-in user may log out. */
fun Route.logoutRoutes(authenticationService: AuthenticationService) {
    post("/logout") {
        authenticationService.logout(call.currentUserId())
        call.respond(HttpStatusCode.NoContent)
    }
}

/**
 * Register inside `authenticate(JWT_AUTHENTICATION)` and a rate limit; every logged-in user may change their own
 * password. The URL sits under `/users/me`, the logic here, because password hashing belongs to authentication.
 */
fun Route.passwordRoutes(authenticationService: AuthenticationService) {
    post("/users/me/password") {
        val request = call.receive<ChangePasswordRequest>()
        call.respond(HttpStatusCode.OK, authenticationService.changePassword(call.currentUserId(), request))
    }
}
