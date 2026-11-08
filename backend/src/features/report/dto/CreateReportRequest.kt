package nl.brul.backend.features.report.dto

import kotlinx.serialization.Serializable

/**
 * Body of `POST /reports`. No owner, status or time: the owner is the caller, every report starts as `OPEN` and the
 * server sets the time. [type], [category] are plain strings so an unknown value gives a clear 400 message.
 * [urgent] is only for `SAFETY`, [volunteersNeeded] only for `HELP_REQUEST`.
 */
@Serializable
data class CreateReportRequest(
    val type: String,
    val title: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val urgent: Boolean? = null,
    val volunteersNeeded: Int? = null
)
