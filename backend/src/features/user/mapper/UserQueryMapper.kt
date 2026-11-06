package nl.brul.backend.features.user.mapper

import io.ktor.http.Parameters
import nl.brul.backend.features.user.model.UserQuery
import nl.brul.backend.features.user.model.UserSortField
import nl.brul.backend.shared.http.booleanOrNull
import nl.brul.backend.shared.http.oneOf
import nl.brul.backend.shared.http.page
import nl.brul.backend.shared.http.searchText
import nl.brul.backend.shared.http.sortDirection

/**
 * Reads the query parameters of `GET /users`, e.g. `?q=alice&active=true&sort=displayName&order=asc&limit=20`.
 * An invalid value gives 400.
 */
fun Parameters.toUserQuery(): UserQuery =
    UserQuery(
        text = searchText("q"),
        active = booleanOrNull("active"),
        sortField = oneOf("sort", UserSortField.entries.associateBy { it.key }) ?: UserSortField.CREATED_AT,
        sortDirection = sortDirection(),
        page = page()
    )
