package nl.brul.backend.features.feedback.model

import nl.brul.backend.shared.paging.Page
import nl.brul.backend.shared.paging.SortDirection
import java.util.UUID

/**
 * What `GET /feedback` asks for: filters, sorting and a page. A filter that is `null` is not applied.
 * The defaults give the newest items first.
 */
data class FeedbackQuery(
    /** Matches part of the message, ignoring case. */
    val text: String? = null,
    val status: FeedbackStatus? = null,
    /** Only items of this owner. The route forces it to the caller for users without `feedback.read`. */
    val userId: UUID? = null,
    val sortField: FeedbackSortField = FeedbackSortField.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val page: Page = Page()
)
