package nl.brul.backend.features.help.repository

import nl.brul.backend.features.help.model.HelpParticipation
import nl.brul.backend.features.help.model.ParticipationStatus
import java.time.Instant
import java.util.UUID

/**
 * Repository interface for managing volunteer participations.
 * Decouples domain and service logic from Exposed and SQL.
 */
interface HelpParticipationRepository {

    fun findById(id: UUID): HelpParticipation?

    fun findByHelpRequest(helpRequestId: UUID): List<HelpParticipation>

    fun findByHelpRequestAndUser(helpRequestId: UUID, userId: UUID): HelpParticipation?

    fun countByHelpRequest(helpRequestId: UUID): Int

    fun save(participation: HelpParticipation): HelpParticipation

    fun updateStatus(
        helpRequestId: UUID,
        userId: UUID,
        status: ParticipationStatus,
        checkedInAt: Instant? = null
    ): Boolean

    fun delete(helpRequestId: UUID, userId: UUID): Boolean

    fun deleteById(id: UUID): Boolean
}
