package nl.brul.backend.features.report.dto

import kotlinx.serialization.Serializable

/**
 * Body of `PUT /reports/{id}`: the whole report again, plus its [status]. The type cannot change; the owner,
 * the creation time and the view count are kept by the server.
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
    val volunteersNeeded: Int? = null
)
