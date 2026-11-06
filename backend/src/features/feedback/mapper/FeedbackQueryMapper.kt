package nl.brul.backend.features.feedback.mapper

import io.ktor.http.Parameters
import nl.brul.backend.features.feedback.model.FeedbackQuery
import nl.brul.backend.features.feedback.model.FeedbackSortField
import nl.brul.backend.features.feedback.model.FeedbackStatus
import nl.brul.backend.shared.http.oneOf
import nl.brul.backend.shared.http.page
import nl.brul.backend.shared.http.searchText
import nl.brul.backend.shared.http.sortDirection
import nl.brul.backend.shared.http.uuidOrNull

/**
 * Reads the query parameters of `GET /feedback`, e.g. `?q=slow&status=OPEN&sort=createdAt&order=desc&limit=20`.
 * An invalid value gives 400.
 */
fun Parameters.toFeedbackQuery(): FeedbackQuery =
    FeedbackQuery(
        text = searchText("q"),
        status = oneOf("status", FeedbackStatus.entries.associateBy { it.name }),
        userId = uuidOrNull("userId"),
        sortField = oneOf("sort", FeedbackSortField.entries.associateBy { it.key }) ?: FeedbackSortField.CREATED_AT,
        sortDirection = sortDirection(),
        page = page()
    )
