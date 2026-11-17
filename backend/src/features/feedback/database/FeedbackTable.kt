package nl.brul.backend.features.feedback.database

import nl.brul.backend.features.feedback.model.Feedback
import nl.brul.backend.features.feedback.model.FeedbackStatus
import nl.brul.backend.features.user.database.UsersTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object FeedbackTable : Table("feedback") {
    val id = uuid("id")
    /** Foreign key: every item belongs to an existing user. Users are never deleted, only deactivated. */
    val ownerId = reference("owner_id", UsersTable.id)
    val message = varchar("message", Feedback.MAX_MESSAGE_LENGTH)
    /** Stored as the enum name (`OPEN`), so the database stays readable. */
    val status = enumerationByName("status", 20, FeedbackStatus::class)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
