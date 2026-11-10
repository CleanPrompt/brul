package nl.brul.backend.features.vote.repository

import nl.brul.backend.features.vote.database.VotesTable
import nl.brul.backend.features.vote.mapper.toVote
import nl.brul.backend.features.vote.model.Vote
import nl.brul.backend.features.vote.model.VoteType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ExposedVoteRepository : VoteRepository {

    override fun findByFeedbackAndUser(feedbackId: UUID, userId: UUID): Vote? = transaction {
        VotesTable.selectAll()
            .where { (VotesTable.feedbackId eq feedbackId) and (VotesTable.userId eq userId) }
            .singleOrNull()
            ?.toVote()
    }

    override fun findByFeedback(feedbackId: UUID): List<Vote> = transaction {
        VotesTable.selectAll()
            .where { VotesTable.feedbackId eq feedbackId }
            .map { it.toVote() }
    }

    override fun save(vote: Vote): Vote {
        transaction {
            VotesTable.insert {
                it[id] = vote.id
                it[feedbackId] = vote.feedbackId
                it[userId] = vote.userId
                it[type] = vote.type
            }
        }
        return vote
    }

    override fun update(feedbackId: UUID, userId: UUID, type: VoteType): Boolean = transaction {
        VotesTable.update({ (VotesTable.feedbackId eq feedbackId) and (VotesTable.userId eq userId) }) {
            it[VotesTable.type] = type
        } > 0
    }

    override fun delete(feedbackId: UUID, userId: UUID): Boolean = transaction {
        VotesTable.deleteWhere {
            (VotesTable.feedbackId eq feedbackId) and (VotesTable.userId eq userId)
        } > 0
    }
}
