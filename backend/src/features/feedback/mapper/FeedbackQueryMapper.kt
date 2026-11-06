package nl.example.backend.features.feedback.mapper

import io.ktor.http.Parameters
import nl.example.backend.features.feedback.model.FeedbackQuery
import nl.example.backend.features.feedback.model.FeedbackSortField
import nl.example.backend.features.feedback.model.FeedbackStatus
import nl.example.backend.shared.http.oneOf
import nl.example.backend.shared.http.page
import nl.example.backend.shared.http.searchText
import nl.example.backend.shared.http.sortDirection
import nl.example.backend.shared.http.uuidOrNull

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
