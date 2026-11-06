package nl.example.backend.features.feedback.database

import nl.example.backend.features.feedback.model.FeedbackStatus
import nl.example.backend.features.user.database.UsersTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object FeedbackTable : Table("feedback") {
    val id = uuid("id")
    /** Foreign key: every item belongs to an existing user. Users are never deleted, only deactivated. */
    val userId = reference("user_id", UsersTable.id)
    val message = varchar("message", 2000)
    /** Stored as the enum name (`OPEN`), so the database stays readable. */
    val status = enumerationByName("status", 20, FeedbackStatus::class)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
