package nl.brul.backend.features.report.model

import nl.brul.backend.shared.error.requireValid
import java.time.Instant
import java.util.UUID

/**
 * A report by a neighbour: something unsafe, something annoying or a request for help ([details] says which).
 * The rules that always hold are checked here, so an invalid report cannot exist.
 */
data class Report(
    val id: UUID,
    /** The user who made the report. Never changes. */
    val ownerId: UUID,
    val title: String,
    val description: String,
    val category: ReportCategory,
    val location: Location,
    val details: ReportDetails,
    val status: ReportStatus,
    /** Set by the server when the report is made (UTC); never changed afterwards. */
    val createdAt: Instant,
    /** How often the report was opened (`GET /reports/{id}`). */
    val viewCount: Int
) {
    val type: ReportType
        get() = details.type

    init {
        requireValid(title.isNotBlank() && title.length <= MAX_TITLE_LENGTH) {
            "Title must contain between 1 and $MAX_TITLE_LENGTH characters"
        }
        requireValid(description.isNotBlank() && description.length <= MAX_DESCRIPTION_LENGTH) {
            "Description must contain between 1 and $MAX_DESCRIPTION_LENGTH characters"
        }
        requireValid(category in type.categories) {
            "Category $category does not belong to $type; choose one of: ${type.categories.joinToString()}"
        }
        requireValid(viewCount >= 0) { "viewCount cannot be negative" }
    }

    companion object {
        /** Same as the column lengths in `ReportsTable`. */
        const val MAX_TITLE_LENGTH = 120
        const val MAX_DESCRIPTION_LENGTH = 2000
    }
}
