package nl.brul.backend.features.help.dto

import kotlinx.serialization.Serializable

@Serializable
data class HelpParticipationResponse(
    val id: String,
    val helpRequestId: String,
    val userId: String,
    val status: String,
    /** ISO 8601 in UTC when checked in, e.g. `2026-11-20T10:05:00Z`, or null if not yet checked in. */
    val checkedInAt: String? = null
)

@Serializable
data class HelpParticipationSummaryResponse(
    val helpRequestId: String,
    val volunteersNeeded: Int,
    val participantCount: Int,
    val availablePlaces: Int,
    val isFull: Boolean
)
