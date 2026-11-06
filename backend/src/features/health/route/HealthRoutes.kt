package nl.example.backend.features.health.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoutes() {
    get("/health") { call.respondText("OK", status = HttpStatusCode.OK) }
}
