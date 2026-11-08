package nl.brul.backend.features.report.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReportResponse(
    val id: String,
    /** The owner's user id. */
    val ownerId: String,
    /** `SAFETY`, `NUISANCE` or `HELP_REQUEST`. */
    val type: String,
    val title: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    /** `OPEN`, `IN_PROGRESS` or `RESOLVED`. */
    val status: String,
    /** Only for `SAFETY`. */
    val urgent: Boolean? = null,
    /** Only for `HELP_REQUEST`. */
    val volunteersNeeded: Int? = null,
    /** ISO 8601 in UTC, e.g. `2026-11-08T09:30:00Z`. */
    val createdAt: String,
    val viewCount: Int
)
