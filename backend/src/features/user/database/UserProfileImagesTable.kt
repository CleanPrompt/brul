package nl.brul.backend.features.user.database

import nl.brul.backend.shared.image.ImageType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * At most one profile image per user: `user_id` is the primary key, so the database itself refuses a second row.
 * The type (JPEG or PNG) is not stored; it follows from the first bytes of [data] (`ImageType.detect`).
 */
object UserProfileImagesTable : Table("user_profile_images") {
    /** Foreign key: the image belongs to an existing user. Users are never deleted, only deactivated. */
    val userId = reference("user_id", UsersTable.id)
    val data = binary("data", ImageType.MAX_BYTES)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(userId)
}
