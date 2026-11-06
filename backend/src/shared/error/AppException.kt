package nl.example.backend.shared.error

/**
 * Expected application errors. `plugins/StatusPages.kt` maps each subtype to an HTTP status code
 * and an [ErrorResponse], so services and routes never build error responses themselves.
 *
 * [code] is a stable, machine-readable identifier; [message] is a human-readable explanation.
 */
sealed class AppException(
    val code: String,
    override val message: String
) : RuntimeException(message) {

    /** 400: the input is invalid. */
    class BadRequest(message: String, code: String = "INVALID_REQUEST") : AppException(code, message)

    /** 401: the caller is not authenticated or the credentials are wrong. */
    class Unauthorized(message: String, code: String = "UNAUTHORIZED") : AppException(code, message)

    /** 403: the caller is authenticated but lacks the required permission. */
    class Forbidden(message: String, code: String = "FORBIDDEN") : AppException(code, message)

    /** 404: the requested resource does not exist. */
    class NotFound(message: String, code: String) : AppException(code, message)

    /** 409: the request conflicts with existing data, e.g. a duplicate email. */
    class Conflict(message: String, code: String) : AppException(code, message)
}

/**
 * Like Kotlin's `require`, but throws [AppException.BadRequest] so the message reaches the client.
 */
inline fun requireValid(condition: Boolean, message: () -> String) {
    if (!condition) {
        throw AppException.BadRequest(message())
    }
}
