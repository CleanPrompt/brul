package nl.brul.backend.features.user

import nl.brul.backend.features.user.model.ProfileImage
import nl.brul.backend.features.user.repository.ProfileImageRepository
import java.util.UUID

/**
 * In-memory [ProfileImageRepository] so service and route tests do not need a database. The user id is the key, so
 * a second save replaces the first, just like the primary key in `ExposedProfileImageRepository`.
 */
class FakeProfileImageRepository : ProfileImageRepository {
    private val images = mutableMapOf<UUID, ProfileImage>()

    override fun save(image: ProfileImage): ProfileImage = image.also { images[it.userId] = it }

    override fun findByUserId(userId: UUID): ProfileImage? = images[userId]

    override fun delete(userId: UUID): Boolean = images.remove(userId) != null
}
