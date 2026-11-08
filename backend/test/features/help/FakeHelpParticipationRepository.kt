package nl.brul.backend.features.help

import nl.brul.backend.features.help.model.HelpParticipation
import nl.brul.backend.features.help.model.ParticipationStatus
import nl.brul.backend.features.help.repository.HelpParticipationRepository
import nl.brul.backend.shared.error.AppException
import java.time.Instant
import java.util.UUID

/**
 * In-memory [HelpParticipationRepository] for fast, deterministic unit and route tests without a database.
 */
class FakeHelpParticipationRepository : HelpParticipationRepository {
    private val items = mutableMapOf<UUID, HelpParticipation>()

    override fun findById(id: UUID): HelpParticipation? = items[id]

    override fun findByHelpRequest(helpRequestId: UUID): List<HelpParticipation> =
        items.values.filter { it.helpRequestId == helpRequestId }

    override fun findByHelpRequestAndUser(helpRequestId: UUID, userId: UUID): HelpParticipation? =
        items.values.find { it.helpRequestId == helpRequestId && it.userId == userId }

    override fun countByHelpRequest(helpRequestId: UUID): Int =
        items.values.count { it.helpRequestId == helpRequestId }

    override fun save(participation: HelpParticipation): HelpParticipation {
        val existing = findByHelpRequestAndUser(participation.helpRequestId, participation.userId)
        if (existing != null) {
            throw AppException.Conflict("You already participate in this help request.", "ALREADY_PARTICIPATING")
        }
        items[participation.id] = participation
        return participation
    }

    override fun updateStatus(
        helpRequestId: UUID,
        userId: UUID,
        status: ParticipationStatus,
        checkedInAt: Instant?
    ): Boolean {
        val existing = findByHelpRequestAndUser(helpRequestId, userId) ?: return false
        items[existing.id] = existing.copy(
            status = status,
            checkedInAt = checkedInAt ?: existing.checkedInAt
        )
        return true
    }

    override fun delete(helpRequestId: UUID, userId: UUID): Boolean {
        val existing = findByHelpRequestAndUser(helpRequestId, userId) ?: return false
        return items.remove(existing.id) != null
    }

    override fun deleteById(id: UUID): Boolean = items.remove(id) != null

    fun clear() {
        items.clear()
    }
}
