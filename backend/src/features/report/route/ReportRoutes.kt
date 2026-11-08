package nl.brul.backend.features.report.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import nl.brul.backend.features.authorization.model.Permission
import nl.brul.backend.features.authorization.service.AuthorizationService
import nl.brul.backend.features.report.dto.CreateReportRequest
import nl.brul.backend.features.report.dto.UpdateReportRequest
import nl.brul.backend.features.report.mapper.toReportQuery
import nl.brul.backend.features.report.service.ReportService
import nl.brul.backend.plugins.currentUserId
import nl.brul.backend.shared.http.uuidParameter

/**
 * Register inside `authenticate(JWT_AUTHENTICATION)`. Every logged-in user may make and read reports; owners may
 * always change and delete their own reports, others need `report.update` or `report.delete`.
 *
 * To know the owner, `getOwnerId` reads the report first, so an unknown id gives 404 before the access check.
 */
fun Route.reportRoutes(reportService: ReportService, authorizationService: AuthorizationService) {
    route("/reports") {
        // The caller becomes the owner.
        post {
            val request = call.receive<CreateReportRequest>()
            call.respond(HttpStatusCode.Created, reportService.create(call.currentUserId(), request))
        }

        // Search, filter, area, sort and page, e.g. ?q=bike&type=SAFETY&latitude=51.58&longitude=4.77&radiusKm=2.
        get {
            call.respond(reportService.search(call.request.queryParameters.toReportQuery()))
        }

        // Reports are visible to every neighbour; opening one counts as a view.
        get("/{id}") {
            call.respond(reportService.getById(call.uuidParameter("id")))
        }

        put("/{id}") {
            val id = call.uuidParameter("id")
            authorizationService.requireOwnerOrPermission(
                call.currentUserId(), reportService.getOwnerId(id), Permission.REPORT_UPDATE
            )
            val request = call.receive<UpdateReportRequest>()
            call.respond(reportService.update(id, request))
        }

        delete("/{id}") {
            val id = call.uuidParameter("id")
            authorizationService.requireOwnerOrPermission(
                call.currentUserId(), reportService.getOwnerId(id), Permission.REPORT_DELETE
            )
            reportService.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
