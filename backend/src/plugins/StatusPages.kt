package nl.brul.backend.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
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

        // Thrown by `call.receive<T>()` when the `Content-Type` header is missing or not JSON (e.g. `text/plain`).
        // Without this handler, the `Throwable` handler below would turn a client error into a 500.
        exception<ContentTransformationException> { call, _ ->
            call.respond(
                HttpStatusCode.UnsupportedMediaType,
                ErrorResponse("UNSUPPORTED_MEDIA_TYPE", "Content-Type must be application/json")
            )
        }

        // Ktor picks the handler of the most specific class, so this one only catches what the others do not.
        exception<Throwable> { call, cause ->
            logger.error("Unhandled request error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("INTERNAL_ERROR", "Internal server error")
            )
        }

        // Responses produced by Ktor itself: missing/invalid JWT, unknown route, wrong method, rate limit.
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(status, ErrorResponse("UNAUTHORIZED", "Authentication required"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse("NOT_FOUND", "Resource not found"))
        }
        // The path exists, but not with this method, e.g. `PUT /health`.
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            call.respond(status, ErrorResponse("METHOD_NOT_ALLOWED", "Method not allowed"))
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
    }
