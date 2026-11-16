package nl.brul.backend.features.user.repository

import nl.brul.backend.features.user.model.ProfileImage
import java.util.UUID

interface ProfileImageRepository {
    /** Stores the image; an existing image of the same user is replaced. */
    fun save(image: ProfileImage): ProfileImage

    fun findByUserId(userId: UUID): ProfileImage?

    /** Returns `false` when the user had no image. */
    fun delete(userId: UUID): Boolean
}
