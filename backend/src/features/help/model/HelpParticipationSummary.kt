package nl.brul.backend.features.help.model

import java.util.UUID

/**
 * Domain model summarizing volunteer availability and capacity for a help request.
 */
data class HelpParticipationSummary(
    val helpRequestId: UUID,
    val volunteersNeeded: Int,
    val participantCount: Int,
    val availablePlaces: Int,
    val isFull: Boolean
)
