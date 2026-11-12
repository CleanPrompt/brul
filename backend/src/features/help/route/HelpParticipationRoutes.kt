package nl.brul.backend.features.help.route

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.brul.backend.features.authorization.service.AuthorizationService
import nl.brul.backend.features.help.dto.CheckInRequest
import nl.brul.backend.features.help.mapper.toResponse
import nl.brul.backend.features.help.model.Location
import nl.brul.backend.features.help.service.HelpParticipationService
import nl.brul.backend.plugins.currentUserId
import nl.brul.backend.shared.http.uuidParameter

/**
 * Public routes: calendar export (.ics) and participant capacity summary.
 */
fun Route.helpParticipationPublicRoutes(service: HelpParticipationService) {
    route("/help-requests/{id}") {
        // RFC 5545 iCalendar feed export (public so calendar apps can subscribe).
        get("/calendar.ics") {
            val id = call.uuidParameter("id")
            val icsContent = service.getCalendarFeed(id)

            call.response.header(
                HttpHeaders.ContentDisposition,
                "attachment; filename=\"help-request-$id.ics\""
            )
            call.respondText(
                text = icsContent,
                contentType = ContentType.parse("text/calendar; charset=utf-8"),
                status = HttpStatusCode.OK
            )
        }

        // Public capacity and availability summary.
        get("/participants/summary") {
            val id = call.uuidParameter("id")
            call.respond(HttpStatusCode.OK, service.getSummary(id).toResponse())
        }
    }
}

/**
 * Protected routes registered inside `authenticate(JWT_AUTHENTICATION)`.
 */
fun Route.helpParticipationRoutes(
    service: HelpParticipationService,
    authorizationService: AuthorizationService
) {
    route("/help-requests/{id}") {

        // Owner generates check-in code (QR/OTP).
        post("/check-in-code") {
            val id = call.uuidParameter("id")
            val currentUserId = call.currentUserId()
            val response = service.generateCheckInCode(id, currentUserId)
            call.respond(HttpStatusCode.OK, response)
        }

        // Volunteer checks in at the help request location.
        post("/check-in") {
            val id = call.uuidParameter("id")
            val currentUserId = call.currentUserId()
            val request = call.receive<CheckInRequest>()
            val location = if (request.latitude != null && request.longitude != null) {
                Location(request.latitude, request.longitude)
            } else {
                null
            }

            val response = service.checkIn(
                helpRequestId = id,
                userId = currentUserId,
                code = request.code,
                volunteerLocation = location
            )
            call.respond(HttpStatusCode.OK, response)
        }

        route("/participants") {
            // View participant list.
            get {
                val id = call.uuidParameter("id")
                val participants = service.getParticipants(id).map { it.toResponse() }
                call.respond(HttpStatusCode.OK, participants)
            }

            // Join a help request as volunteer.
            post {
                val id = call.uuidParameter("id")
                val currentUserId = call.currentUserId()
                val participation = service.join(id, currentUserId)
                call.respond(HttpStatusCode.Created, participation.toResponse())
            }

            // Leave / cancel own participation.
            delete("/me") {
                val id = call.uuidParameter("id")
                val currentUserId = call.currentUserId()
                service.leave(id, currentUserId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
