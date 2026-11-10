package nl.brul.backend.features.vote.database

import nl.brul.backend.features.feedback.database.FeedbackTable
import nl.brul.backend.features.user.database.UsersTable
import nl.brul.backend.features.vote.model.VoteType
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object VotesTable : Table("votes") {
    val id = uuid("id")
    val feedbackId = reference("feedback_id", FeedbackTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val type = enumerationByName("type", 10, VoteType::class)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("uq_votes_feedback_user", feedbackId, userId)
        index(false, feedbackId)
    }
}
