package nl.brul.backend.features.user.mapper

import io.ktor.http.ContentType
import nl.brul.backend.features.user.model.ProfileImage
import nl.brul.backend.shared.image.ImageType

/**
 * The `Content-Type` to send with the image. It is not stored (it follows from the bytes), so it is read from the
 * bytes again. Every image was checked on upload, so `null` here means a bug: `error()` gives a 500.
 */
fun ProfileImage.contentType(): ContentType {
    val type = ImageType.detect(data)
        ?: error("Stored image is not a JPEG or PNG")
    return ContentType.parse(type.contentType)
}
