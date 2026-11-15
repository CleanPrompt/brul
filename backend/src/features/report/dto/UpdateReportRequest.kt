package nl.brul.backend.features.report.dto

import kotlinx.serialization.Serializable

/**
 * Body of `PUT /reports/{id}`: the whole report again, plus its [status]. The type cannot change; the owner,
 * the creation time and the view count are kept by the server.
 * Planning is replaced too: omit both startTime and endTime (or send both as null) to clear it.
 * To keep existing planning, send both existing ISO 8601 values. Sending only one gives 400.
 */
@Serializable
data class UpdateReportRequest(
    val type: String,
    val title: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val urgent: Boolean? = null,
    val volunteersNeeded: Int? = null,
    /** Optional ISO 8601 instants; both must be provided together, only for HELP_REQUEST. */
    val startTime: String? = null,
    val endTime: String? = null
)
