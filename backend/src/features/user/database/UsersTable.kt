package nl.brul.backend.features.user.database

import nl.brul.backend.features.user.model.User
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 320).uniqueIndex()
    val displayName = varchar("display_name", User.MAX_DISPLAY_NAME_LENGTH)
    val passwordHash = varchar("password_hash", 255)
    val active = bool("active").default(true)
    val createdAt = timestamp("created_at")
    val tokenVersion = integer("token_version").default(0)

    override val primaryKey = PrimaryKey(id)
}
