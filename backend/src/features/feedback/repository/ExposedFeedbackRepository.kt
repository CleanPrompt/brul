package nl.example.backend.features.feedback.repository

import nl.example.backend.features.feedback.database.FeedbackTable
import nl.example.backend.features.feedback.mapper.toFeedback
import nl.example.backend.features.feedback.model.Feedback
import nl.example.backend.features.feedback.model.FeedbackQuery
import nl.example.backend.features.feedback.model.FeedbackSortField
import nl.example.backend.shared.database.containsIgnoringCase
import nl.example.backend.shared.database.toSortOrder
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ExposedFeedbackRepository : FeedbackRepository {
    override fun findById(id: UUID): Feedback? = transaction {
        FeedbackTable.selectAll().where { FeedbackTable.id eq id }
            .singleOrNull()?.toFeedback()
    }

    override fun search(query: FeedbackQuery): List<Feedback> = transaction {
        filtered(query)
            // The id as a second sort key keeps the order stable, so no item appears on two pages.
            .orderBy(
                sortColumn(query.sortField) to query.sortDirection.toSortOrder(),
                FeedbackTable.id to SortOrder.ASC
            )
            .limit(query.page.limit)
            .offset(query.page.offset.toLong())
            .map { it.toFeedback() }
    }

    override fun count(query: FeedbackQuery): Long = transaction {
        filtered(query).count()
    }

    /** Starts with every row and adds one `AND` condition for each filter that is set. */
    private fun filtered(query: FeedbackQuery): Query {
        val rows = FeedbackTable.selectAll()
        query.userId?.let { userId -> rows.andWhere { FeedbackTable.userId eq userId } }
        query.status?.let { status -> rows.andWhere { FeedbackTable.status eq status } }
        query.text?.let { text -> rows.andWhere { FeedbackTable.message.containsIgnoringCase(text) } }
        return rows
    }

    private fun sortColumn(field: FeedbackSortField): Column<*> =
        when (field) {
            FeedbackSortField.CREATED_AT -> FeedbackTable.createdAt
            FeedbackSortField.STATUS -> FeedbackTable.status
        }

    override fun save(feedback: Feedback): Feedback {
        transaction {
            FeedbackTable.insert {
                it[id] = feedback.id
                it[userId] = feedback.userId
                it[message] = feedback.message
                it[status] = feedback.status
                it[createdAt] = feedback.createdAt
            }
        }
        return feedback
    }

    override fun update(feedback: Feedback): Boolean = transaction {
        FeedbackTable.update({ FeedbackTable.id eq feedback.id }) {
            it[message] = feedback.message
            it[status] = feedback.status
        } > 0
    }

    override fun delete(id: UUID): Boolean = transaction {
        FeedbackTable.deleteWhere { FeedbackTable.id eq id } > 0
    }
}
