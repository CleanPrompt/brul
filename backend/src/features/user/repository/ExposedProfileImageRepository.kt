package nl.brul.backend.features.user.repository

import nl.brul.backend.features.user.database.UserProfileImagesTable
import nl.brul.backend.features.user.mapper.toProfileImage
import nl.brul.backend.features.user.model.ProfileImage
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ExposedProfileImageRepository : ProfileImageRepository {
    /**
     * Replaces the image: delete the old row (if there is one), then insert the new one. Both happen in one
     * transaction, so they succeed or fail together. Not `upsert`: in Exposed 0.61 on H2 it damages binary data.
     */
    override fun save(image: ProfileImage): ProfileImage {
        transaction {
            UserProfileImagesTable.deleteWhere { UserProfileImagesTable.userId eq image.userId }
            UserProfileImagesTable.insert {
                it[userId] = image.userId
                it[data] = image.data
                it[createdAt] = image.createdAt
            }
        }
        return image
    }

    override fun findByUserId(userId: UUID): ProfileImage? = transaction {
        UserProfileImagesTable.selectAll().where { UserProfileImagesTable.userId eq userId }
            .singleOrNull()
            ?.toProfileImage()
    }

    override fun delete(userId: UUID): Boolean = transaction {
        UserProfileImagesTable.deleteWhere { UserProfileImagesTable.userId eq userId } > 0
    }
}
