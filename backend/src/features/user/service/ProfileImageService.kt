package nl.brul.backend.features.user.service

import nl.brul.backend.features.user.model.ProfileImage
import nl.brul.backend.features.user.repository.ProfileImageRepository
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.image.requireValidImage
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Business rules for profile images. Access checks happen in the routes, through `AuthorizationService`; the route
 * also checks that the user exists (`UserService.getOwnerId`) before it calls this service.
 */
class ProfileImageService(private val repository: ProfileImageRepository) {
    /** Checks the image (type and size, 400 otherwise) and stores it; an existing image of the user is replaced. */
    fun upload(userId: UUID, bytes: ByteArray) {
        requireValidImage(bytes)

        val image = ProfileImage(
            userId = userId,
            data = bytes,
            createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        repository.save(image)
    }

    fun get(userId: UUID): ProfileImage =
        repository.findByUserId(userId) ?: throw profileImageNotFound()

    fun delete(userId: UUID) {
        if (!repository.delete(userId)) {
            throw profileImageNotFound()
        }
    }

    private fun profileImageNotFound() =
        AppException.NotFound("Profile image not found", "PROFILE_IMAGE_NOT_FOUND")
}
