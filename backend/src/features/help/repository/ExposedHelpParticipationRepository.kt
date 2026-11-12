package nl.brul.backend.features.help.repository

import nl.brul.backend.features.help.database.HelpParticipationsTable
import nl.brul.backend.features.help.mapper.toHelpParticipation
import nl.brul.backend.features.help.model.HelpParticipation
import nl.brul.backend.features.help.model.ParticipationStatus
import nl.brul.backend.shared.error.AppException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ExposedHelpParticipationRepository : HelpParticipationRepository {

    override fun findById(id: UUID): HelpParticipation? = transaction {
        HelpParticipationsTable
            .selectAll()
            .where { HelpParticipationsTable.id eq id }
            .singleOrNull()
            ?.toHelpParticipation()
    }

    override fun findByHelpRequest(helpRequestId: UUID): List<HelpParticipation> = transaction {
        HelpParticipationsTable
            .selectAll()
            .where { HelpParticipationsTable.helpRequestId eq helpRequestId }
            .orderBy(HelpParticipationsTable.id to SortOrder.ASC)
            .map { it.toHelpParticipation() }
    }

    override fun findByHelpRequestAndUser(helpRequestId: UUID, userId: UUID): HelpParticipation? = transaction {
        HelpParticipationsTable
            .selectAll()
            .where {
                (HelpParticipationsTable.helpRequestId eq helpRequestId) and
                    (HelpParticipationsTable.userId eq userId)
            }
            .singleOrNull()
            ?.toHelpParticipation()
    }

    override fun countByHelpRequest(helpRequestId: UUID): Int = transaction {
        HelpParticipationsTable
            .selectAll()
            .where { HelpParticipationsTable.helpRequestId eq helpRequestId }
            .count()
            .toInt()
    }

    override fun save(participation: HelpParticipation): HelpParticipation = transaction {
        maxAttempts = 1
        try {
            HelpParticipationsTable.insert {
                it[id] = participation.id
                it[helpRequestId] = participation.helpRequestId
                it[userId] = participation.userId
                it[status] = participation.status
                it[checkedInAt] = participation.checkedInAt
            }
            participation
        } catch (e: ExposedSQLException) {
            if (e.sqlState == "23505") {
                throw AppException.Conflict("You already participate in this help request.", "ALREADY_PARTICIPATING")
            }
            throw e
        }
    }

    override fun updateStatus(
        helpRequestId: UUID,
        userId: UUID,
        status: ParticipationStatus,
        checkedInAt: Instant?
    ): Boolean = transaction {
        HelpParticipationsTable.update({
            (HelpParticipationsTable.helpRequestId eq helpRequestId) and
                (HelpParticipationsTable.userId eq userId)
        }) {
            it[HelpParticipationsTable.status] = status
            if (checkedInAt != null) {
                it[HelpParticipationsTable.checkedInAt] = checkedInAt
            }
        } > 0
    }

    override fun delete(helpRequestId: UUID, userId: UUID): Boolean = transaction {
        HelpParticipationsTable.deleteWhere {
            (HelpParticipationsTable.helpRequestId eq helpRequestId) and
                (HelpParticipationsTable.userId eq userId)
        } > 0
    }

    override fun deleteById(id: UUID): Boolean = transaction {
        HelpParticipationsTable.deleteWhere { HelpParticipationsTable.id eq id } > 0
    }
}
