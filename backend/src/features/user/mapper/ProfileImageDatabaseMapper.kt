package nl.brul.backend.features.user.mapper

import nl.brul.backend.features.user.database.UserProfileImagesTable
import nl.brul.backend.features.user.model.ProfileImage
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toProfileImage(): ProfileImage =
    ProfileImage(
        userId = this[UserProfileImagesTable.userId],
        data = this[UserProfileImagesTable.data],
        createdAt = this[UserProfileImagesTable.createdAt]
    )
