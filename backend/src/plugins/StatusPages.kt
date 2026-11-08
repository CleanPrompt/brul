package nl.brul.backend.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.error.ErrorResponse

fun Application.configureStatusPages() {
    val logger = environment.log

    install(StatusPages) {
        exception<AppException> { call, cause ->
            call.respond(cause.toHttpStatus(), ErrorResponse(cause.code, cause.message))
        }

        // Thrown by Ktor when the request body is missing or cannot be deserialized.
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("INVALID_REQUEST", "Request body is missing or malformed")
            )
        }

        exception<Throwable> { call, cause ->
            logger.error("Unhandled request error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("INTERNAL_ERROR", "Internal server error")
            )
        }

        // Responses produced by Ktor itself: missing/invalid JWT, unknown route, rate limit.
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(status, ErrorResponse("UNAUTHORIZED", "Authentication required"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse("NOT_FOUND", "Resource not found"))
        }
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status, ErrorResponse("TOO_MANY_REQUESTS", "Too many requests, try again later"))
        }
    }
}

private fun AppException.toHttpStatus(): HttpStatusCode =
    when (this) {
        is AppException.BadRequest -> HttpStatusCode.BadRequest
        is AppException.Unauthorized -> HttpStatusCode.Unauthorized
        is AppException.Forbidden -> HttpStatusCode.Forbidden
        is AppException.NotFound -> HttpStatusCode.NotFound
        is AppException.Conflict -> HttpStatusCode.Conflict
        is AppException.PayloadTooLarge -> HttpStatusCode.PayloadTooLarge
        is AppException.UnsupportedMediaType -> HttpStatusCode.UnsupportedMediaType
    }
