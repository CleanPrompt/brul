package nl.brul.backend.shared.http

import io.ktor.http.Parameters
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.error.requireValid
import nl.brul.backend.shared.paging.Page
import nl.brul.backend.shared.paging.SortDirection
import java.util.UUID

/*
 * Readers for optional query parameters such as `?q=alice&limit=20`. A missing parameter gives `null` or a default;
 * an invalid value gives 400 with the name of the parameter, never a silent fallback.
 */

/** Longest accepted search text; longer input is almost certainly not a real search. */
const val MAX_SEARCH_LENGTH = 100

/** Free search text, trimmed. Missing or empty means "no text filter". */
fun Parameters.searchText(name: String): String? {
    val text = this[name]?.trim()
    if (text.isNullOrEmpty()) {
        return null
    }
    requireValid(text.length <= MAX_SEARCH_LENGTH) { "$name may contain at most $MAX_SEARCH_LENGTH characters" }
    return text
}

fun Parameters.uuidOrNull(name: String): UUID? =
    this[name]?.let { parseUuid(it, name) }

fun Parameters.booleanOrNull(name: String): Boolean? =
    when (this[name]) {
        null -> null
        "true" -> true
        "false" -> false
        else -> throw AppException.BadRequest("$name must be true or false")
    }

/**
 * A value from a fixed list: [options] maps each accepted text to its value,
 * e.g. `mapOf("asc" to SortDirection.ASC, "desc" to SortDirection.DESC)`.
 */
fun <T> Parameters.oneOf(name: String, options: Map<String, T>): T? {
    val value = this[name] ?: return null
    return options[value] ?: throw AppException.BadRequest("$name must be one of: ${options.keys.joinToString()}")
}

/** `order=asc` or `order=desc`; newest-first lists use the default [SortDirection.DESC]. */
fun Parameters.sortDirection(): SortDirection =
    oneOf("order", mapOf("asc" to SortDirection.ASC, "desc" to SortDirection.DESC)) ?: SortDirection.DESC

/** `limit` (1 to [Page.MAX_LIMIT], default [Page.DEFAULT_LIMIT]) and `offset` (0 or more). */
fun Parameters.page(): Page {
    val limit = intOrNull("limit") ?: Page.DEFAULT_LIMIT
    val offset = intOrNull("offset") ?: 0
    requireValid(limit in 1..Page.MAX_LIMIT) { "limit must be between 1 and ${Page.MAX_LIMIT}" }
    requireValid(offset >= 0) { "offset must be 0 or more" }
    return Page(limit, offset)
}

private fun Parameters.intOrNull(name: String): Int? =
    this[name]?.let { it.toIntOrNull() ?: throw AppException.BadRequest("$name must be a whole number") }
