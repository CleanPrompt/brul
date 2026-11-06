package nl.example.backend.features.authorization.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import nl.example.backend.features.authorization.dto.AssignRoleRequest
import nl.example.backend.features.authorization.model.Permission
import nl.example.backend.features.authorization.service.AuthorizationService
import nl.example.backend.features.authorization.service.RoleService
import nl.example.backend.plugins.currentUserId
import nl.example.backend.shared.http.parseUuid
import nl.example.backend.shared.http.uuidParameter

/** Register inside `authenticate(JWT_AUTHENTICATION)`. */
fun Route.roleRoutes(roleService: RoleService, authorizationService: AuthorizationService) {
    get("/roles") {
        authorizationService.requirePermission(call.currentUserId(), Permission.ROLE_READ)
        call.respond(roleService.findAllRoles())
    }

    get("/users/{id}/roles") {
        val userId = call.uuidParameter("id")
        // Users own their account, so they may always see their own roles.
        authorizationService.requireOwnerOrPermission(call.currentUserId(), userId, Permission.ROLE_READ)
        call.respond(roleService.findRolesForUser(userId))
    }

    post("/users/{id}/roles") {
        authorizationService.requirePermission(call.currentUserId(), Permission.ROLE_ASSIGN)
        val userId = call.uuidParameter("id")
        val request = call.receive<AssignRoleRequest>()

        roleService.assignRole(userId, parseUuid(request.roleId, "roleId"))
        call.respond(HttpStatusCode.NoContent)
    }

    delete("/users/{id}/roles/{roleId}") {
        val currentUserId = call.currentUserId()
        authorizationService.requirePermission(currentUserId, Permission.ROLE_ASSIGN)

        roleService.removeRole(call.uuidParameter("id"), call.uuidParameter("roleId"), currentUserId)
        call.respond(HttpStatusCode.NoContent)
    }
}
