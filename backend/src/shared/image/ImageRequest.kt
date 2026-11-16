package nl.brul.backend.shared.image

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentLength
import io.ktor.server.request.receive
import nl.brul.backend.shared.error.AppException

/**
 * Reads the request body as the bytes of an image. The size is checked with the `Content-Length` header *before*
 * the body is read, so a client cannot make the server load a huge body into memory. Without that header the
 * server cannot know the size, so the request is refused. Type and real size are checked by `requireValidImage`.
 */
suspend fun ApplicationCall.receiveImage(): ByteArray {
    val length = request.contentLength()
        ?: throw AppException.BadRequest("The Content-Length header is required")

    if (length > ImageType.MAX_BYTES) {
        throw AppException.BadRequest("An image can be at most 2 MB", "IMAGE_TOO_LARGE")
    }

    return receive<ByteArray>()
}
