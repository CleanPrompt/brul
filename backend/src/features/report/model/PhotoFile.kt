package nl.brul.backend.features.report.model

/** The image of a photo, ready to send to the client. */
class PhotoFile(
    val contentType: String,
    val bytes: ByteArray
)
