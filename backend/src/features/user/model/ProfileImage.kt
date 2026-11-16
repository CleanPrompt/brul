package nl.brul.backend.features.user.model

import java.time.Instant
import java.util.UUID

/**
 * The profile image of one user.
 *
 * A normal class, not a data class: a data class compares a `ByteArray` by reference, not by content, so two images
 * with the same bytes would not be equal. Tests compare the bytes with `contentEquals` instead.
 */
class ProfileImage(
    val userId: UUID,
    /** A JPEG or PNG of at most `ImageType.MAX_BYTES`, checked before it is saved. */
    val data: ByteArray,
    /** Set by the server when the image is uploaded (UTC). */
    val createdAt: Instant
)
