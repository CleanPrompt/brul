package nl.example.backend.shared.http

import io.ktor.server.application.ApplicationCall
import nl.example.backend.shared.error.AppException
import java.util.UUID

/** Parses [value] as a UUID, or throws 400 with the name of the [field]. */
fun parseUuid(value: String?, field: String): UUID =
    value
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw AppException.BadRequest("Invalid $field")

/** Reads a UUID path parameter such as `{id}`, or throws 400. */
fun ApplicationCall.uuidParameter(name: String): UUID =
    parseUuid(parameters[name], name)
