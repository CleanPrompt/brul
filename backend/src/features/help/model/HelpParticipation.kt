package nl.brul.backend.features.help.model

import java.time.Instant
import java.util.UUID

/**
 * Domain model representing a volunteer's registration for a help request.
 */
data class HelpParticipation(
    val id: UUID,
    val helpRequestId: UUID,
    /** The owner of this participation: the volunteer who joined. */
    val userId: UUID,
    val status: ParticipationStatus = ParticipationStatus.SIGNED_UP,
    /** Set when the volunteer successfully checks in; null otherwise. */
    val checkedInAt: Instant? = null
)
