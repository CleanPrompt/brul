package nl.brul.backend.features.user.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import nl.brul.backend.features.authorization.model.Permission
import nl.brul.backend.features.authorization.service.AuthorizationService
import nl.brul.backend.features.user.mapper.contentType
import nl.brul.backend.features.user.service.ProfileImageService
import nl.brul.backend.features.user.service.UserService
import nl.brul.backend.plugins.currentUserId
import nl.brul.backend.shared.http.uuidParameter
import nl.brul.backend.shared.image.receiveImage

/**
 * Register inside `authenticate(JWT_AUTHENTICATION)`. Whoever may change a user may change their profile image:
 * the account itself, or someone with `user.update`. Every logged-in user may see a profile image.
 */
fun Route.profileImageRoutes(
    profileImageService: ProfileImageService,
    userService: UserService,
    authorizationService: AuthorizationService
) {
    route("/users/{id}/profile-image") {
        // The body is the image file itself (raw bytes), not JSON.
        put {
            val id = call.uuidParameter("id")
            authorizationService.requireOwnerOrPermission(
                call.currentUserId(), userService.getOwnerId(id), Permission.USER_UPDATE
            )
            profileImageService.upload(id, call.receiveImage())
            call.respond(HttpStatusCode.NoContent)
        }

        // Every logged-in user may see any profile image; no permission needed.
        get {
            val image = profileImageService.get(call.uuidParameter("id"))
            call.respondBytes(image.data, image.contentType())
        }

        delete {
            val id = call.uuidParameter("id")
            authorizationService.requireOwnerOrPermission(
                call.currentUserId(), userService.getOwnerId(id), Permission.USER_UPDATE
            )
            profileImageService.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
