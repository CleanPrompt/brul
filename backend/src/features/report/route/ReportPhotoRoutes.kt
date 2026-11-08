package nl.brul.backend.features.report.route

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentLength
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray
import nl.brul.backend.features.authorization.model.Permission
import nl.brul.backend.features.authorization.service.AuthorizationService
import nl.brul.backend.features.report.mapper.appLocationOf
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.service.PhotoRules
import nl.brul.backend.features.report.service.ReportPhotoService
import nl.brul.backend.plugins.currentUserId
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.http.uuidParameter

/** Room for the multipart boundaries and the form fields next to the photo. */
private const val MULTIPART_OVERHEAD_BYTES = 64L * 1024

/**
 * Photos of reports. Register inside `authenticate(JWT_AUTHENTICATION)`. Every logged-in user may see them;
 * adding needs the owner of the report (or `report.update`), deleting the uploader (or `report.photo.delete`).
 * The route reads the report or photo first, so an unknown id gives 404 before the access check.
 */
fun Route.reportPhotoRoutes(photoService: ReportPhotoService, authorizationService: AuthorizationService) {
    get("/reports/{id}/photos") {
        call.respond(photoService.listByReport(call.uuidParameter("id")))
    }

    // Multipart with the file in `photo` and, optionally, `latitude` and `longitude` from the app.
    post("/reports/{id}/photos") {
        val currentUserId = call.currentUserId()
        val reportId = call.uuidParameter("id")
        authorizationService.requireOwnerOrPermission(
            currentUserId, photoService.getReportOwnerId(reportId), Permission.REPORT_UPDATE
        )

        val upload = call.receivePhotoUpload()
        val photo = photoService.upload(reportId, currentUserId, upload.bytes, upload.appLocation)
        call.respond(HttpStatusCode.Created, photo)
    }

    get("/photos/{id}") {
        val image = photoService.getImage(call.uuidParameter("id"))
        call.respondBytes(image.bytes, ContentType.parse(image.contentType))
    }

    delete("/photos/{id}") {
        val id = call.uuidParameter("id")
        authorizationService.requireOwnerOrPermission(
            call.currentUserId(), photoService.getUploaderId(id), Permission.REPORT_PHOTO_DELETE
        )
        photoService.delete(id)
        call.respond(HttpStatusCode.NoContent)
    }
}

/** What an upload form contains. */
private class PhotoUpload(
    val bytes: ByteArray,
    val appLocation: Location?
)

/**
 * Reads the upload form. A request that is clearly too large is refused before it is read, and of the photo
 * the server never reads more than 5 MB + 1 byte: enough to know it is too large, so nobody can fill the memory.
 */
private suspend fun ApplicationCall.receivePhotoUpload(): PhotoUpload {
    val contentLength = request.contentLength()

    if (contentLength != null && contentLength > PhotoRules.MAX_PHOTO_BYTES + MULTIPART_OVERHEAD_BYTES) {
        throw PhotoRules.tooLarge()
    }

    var bytes: ByteArray? = null
    var latitude: String? = null
    var longitude: String? = null

    receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FormItem -> when (part.name) {
                "latitude" -> latitude = part.value
                "longitude" -> longitude = part.value
            }

            is PartData.FileItem -> if (part.name == "photo") {
                bytes = part.provider().readBuffer(PhotoRules.MAX_PHOTO_BYTES + 1).readByteArray()
            }

            else -> Unit
        }

        part.release()
    }

    val photoBytes = bytes

    if (photoBytes == null || photoBytes.isEmpty()) {
        throw AppException.BadRequest("The file field 'photo' is required", "PHOTO_REQUIRED")
    }

    return PhotoUpload(photoBytes, appLocationOf(latitude, longitude))
}
