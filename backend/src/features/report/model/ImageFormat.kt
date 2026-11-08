package nl.brul.backend.features.report.model

/**
 * The file formats accepted for photos.
 *
 * The format is recognised by the first bytes of the file (the "magic numbers"), not by the file name or the
 * `Content-Type` header, because a client can put anything in those.
 */
enum class ImageFormat(
    val contentType: String,
    val extension: String
) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png");

    companion object {
        private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

        private val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )

        /** The format of these bytes, or `null` when they are not a JPEG or PNG. */
        fun detect(bytes: ByteArray): ImageFormat? = when {
            bytes.startsWith(JPEG_SIGNATURE) -> JPEG
            bytes.startsWith(PNG_SIGNATURE) -> PNG
            else -> null
        }

        private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
            size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
    }
}
