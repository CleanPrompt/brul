package nl.brul.backend.features.vote.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import nl.brul.backend.features.vote.dto.VoteRequest
import nl.brul.backend.features.vote.service.VoteService
import nl.brul.backend.plugins.JWT_AUTHENTICATION
import nl.brul.backend.plugins.currentUserId
import nl.brul.backend.shared.http.uuidParameter

fun Route.voteRoutes(voteService: VoteService) {
    route("/feedback/{id}") {
        get("/votes") {
            val feedbackId = call.uuidParameter("id")
            call.respond(voteService.getSummary(feedbackId))
        }

        authenticate(JWT_AUTHENTICATION) {
            put("/vote") {
                val feedbackId = call.uuidParameter("id")
                val request = call.receive<VoteRequest>()
                val (vote, isCreated) = voteService.castVote(feedbackId, call.currentUserId(), request)
                call.respond(if (isCreated) HttpStatusCode.Created else HttpStatusCode.OK, vote)
            }

            get("/vote/me") {
                val feedbackId = call.uuidParameter("id")
                call.respond(voteService.getUserVote(feedbackId, call.currentUserId()))
            }

            delete("/vote") {
                val feedbackId = call.uuidParameter("id")
                voteService.removeVote(feedbackId, call.currentUserId())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
