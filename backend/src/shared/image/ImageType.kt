package nl.brul.backend.shared.image

import nl.brul.backend.shared.error.AppException

/** The image types the API accepts. [contentType] is the value of the `Content-Type` header when we send one back. */
enum class ImageType(val contentType: String) {
    JPEG("image/jpeg"),
    PNG("image/png");

    companion object {
        /** 2 MB. The app makes a photo smaller before it uploads it. */
        const val MAX_BYTES = 2 * 1024 * 1024

        // Every JPEG file starts with these bytes, and every PNG file with 0x89 followed by the letters "PNG".
        private val JPEG_START = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_START = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        /**
         * The type of an image, read from its first bytes, or `null` when it is not a JPEG or PNG.
         * We do not trust the `Content-Type` header or the file name: a client can put anything in those.
         */
        fun detect(bytes: ByteArray): ImageType? =
            when {
                startsWith(bytes, JPEG_START) -> JPEG
                startsWith(bytes, PNG_START) -> PNG
                else -> null
            }

        private fun startsWith(bytes: ByteArray, start: ByteArray): Boolean {
            if (bytes.size < start.size) {
                return false
            }
            for (i in start.indices) {
                if (bytes[i] != start[i]) {
                    return false
                }
            }
            return true
        }
    }
}

/** Returns the type of a valid image, or throws 400 with a code that tells the app what is wrong. */
fun requireValidImage(bytes: ByteArray): ImageType {
    if (bytes.size > ImageType.MAX_BYTES) {
        throw AppException.BadRequest("An image can be at most 2 MB", "IMAGE_TOO_LARGE")
    }

    return ImageType.detect(bytes)
        ?: throw AppException.BadRequest("An image must be a JPEG or PNG", "INVALID_IMAGE_TYPE")
}
