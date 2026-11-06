package nl.example.backend.features.user.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.example.backend.features.authorization.model.Permission
import nl.example.backend.features.authorization.service.AuthorizationService
import nl.example.backend.features.user.dto.UpdateUserRequest
import nl.example.backend.features.user.mapper.toUserQuery
import nl.example.backend.features.user.service.UserService
import nl.example.backend.plugins.currentUserId
import nl.example.backend.shared.http.uuidParameter

/**
 * Register inside `authenticate(JWT_AUTHENTICATION)`. The permission check runs before the lookup,
 * so a caller without access gets 403 and cannot find out whether an id exists.
 * Users own their account, so for `{id}` the account id is also the owner id.
 */
fun Route.userRoutes(userService: UserService, authorizationService: AuthorizationService) {
    route("/users") {
        get("/me") {
            call.respond(userService.getById(call.currentUserId()))
        }

        // Search, filter, sort and page, e.g. ?q=alice&active=true&sort=displayName&order=asc&limit=20&offset=0.
        get {
            authorizationService.requirePermission(call.currentUserId(), Permission.USER_READ)
            val query = call.request.queryParameters.toUserQuery()
            call.respond(userService.search(query))
        }

        get("/{id}") {
            val id = call.uuidParameter("id")
            authorizationService.requireOwnerOrPermission(call.currentUserId(), id, Permission.USER_READ)
            call.respond(userService.getById(id))
        }

        patch("/{id}") {
            val id = call.uuidParameter("id")
            authorizationService.requireOwnerOrPermission(call.currentUserId(), id, Permission.USER_UPDATE)
            val request = call.receive<UpdateUserRequest>()
            call.respond(userService.updateDisplayName(id, request))
        }

        delete("/{id}") {
            val currentUserId = call.currentUserId()
            authorizationService.requirePermission(currentUserId, Permission.USER_DELETE)
            userService.deactivate(call.uuidParameter("id"), currentUserId)
            call.respond(HttpStatusCode.NoContent)
        }

        // Whoever may deactivate a user may also undo it, so this uses the same permission.
        post("/{id}/reactivate") {
            authorizationService.requirePermission(call.currentUserId(), Permission.USER_DELETE)
            userService.reactivate(call.uuidParameter("id"))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
