package nl.example.backend.features.feedback.route

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
import nl.example.backend.features.feedback.dto.FeedbackRequest
import nl.example.backend.features.feedback.dto.UpdateFeedbackStatusRequest
import nl.example.backend.features.feedback.mapper.toFeedbackQuery
import nl.example.backend.features.feedback.service.FeedbackService
import nl.example.backend.plugins.currentUserId
import nl.example.backend.shared.http.uuidParameter

/**
 * Register inside `authenticate(JWT_AUTHENTICATION)`. Owners may always read, edit and delete their own items;
 * for other people's items the caller needs the matching `feedback.*` permission.
 *
 * To know the owner, `getOwnerId` reads the item first, so an unknown id gives 404 before the access check.
 * Ids are random UUIDs, so this reveals nothing useful.
 */
fun Route.feedbackRoutes(feedbackService: FeedbackService, authorizationService: AuthorizationService) {
    route("/feedback") {
        // Every authenticated user may create feedback; the caller becomes the owner.
        post {
            val request = call.receive<FeedbackRequest>()
            call.respond(HttpStatusCode.Created, feedbackService.create(call.currentUserId(), request))
        }

        // Search, filter, sort and page, e.g. ?q=slow&status=OPEN&sort=createdAt&order=desc&limit=20&offset=0.
        get {
            val currentUserId = call.currentUserId()
            val query = call.request.queryParameters.toFeedbackQuery()
            // Filters only narrow the result: without feedback.read the search stays within the caller's own
            // items, whatever userId the caller sent.
            val allowedQuery = if (authorizationService.hasPermission(currentUserId, Permission.FEEDBACK_READ)) {
                query
            } else {
                query.copy(userId = currentUserId)
            }
            call.respond(feedbackService.search(allowedQuery))
        }

        get("/{id}") {
            val id = call.uuidParameter("id")
            authorizationService.requireOwnerOrPermission(
                call.currentUserId(), feedbackService.getOwnerId(id), Permission.FEEDBACK_READ
            )
            call.respond(feedbackService.getById(id))
        }

        patch("/{id}") {
            val id = call.uuidParameter("id")
            authorizationService.requireOwnerOrPermission(
                call.currentUserId(), feedbackService.getOwnerId(id), Permission.FEEDBACK_UPDATE
            )
            val request = call.receive<FeedbackRequest>()
            call.respond(feedbackService.updateMessage(id, request))
        }

        // Only handlers change the status, so owners need the permission as well.
        patch("/{id}/status") {
            authorizationService.requirePermission(call.currentUserId(), Permission.FEEDBACK_UPDATE)
            val request = call.receive<UpdateFeedbackStatusRequest>()
            call.respond(feedbackService.updateStatus(call.uuidParameter("id"), request))
        }

        delete("/{id}") {
            val id = call.uuidParameter("id")
            authorizationService.requireOwnerOrPermission(
                call.currentUserId(), feedbackService.getOwnerId(id), Permission.FEEDBACK_DELETE
            )
            feedbackService.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
