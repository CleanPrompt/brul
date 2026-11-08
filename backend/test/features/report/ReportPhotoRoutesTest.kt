package nl.brul.backend.features.report

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.brul.backend.TEST_ADMIN_EMAIL
import nl.brul.backend.TestServices
import nl.brul.backend.configureTestApp
import nl.brul.backend.errorCode
import nl.brul.backend.features.authentication.dto.AuthenticationResponse
import nl.brul.backend.features.report.dto.ReportPhotoResponse
import nl.brul.backend.features.report.dto.ReportResponse
import nl.brul.backend.features.report.service.PhotoRules
import nl.brul.backend.register
import java.util.UUID
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Photos of reports through the API, with `TestServices` (fakes and in-memory photo files). */
class ReportPhotoRoutesTest {

    /** Registers alice, bob and an admin, and lets alice make one report. */
    private class Setup(
        val services: TestServices,
        val alice: AuthenticationResponse,
        val bob: AuthenticationResponse,
        val admin: AuthenticationResponse,
        val reportId: String
    )

    private fun testPhotos(block: suspend ApplicationTestBuilder.(Setup) -> Unit) = testApplication {
        val services = TestServices()
        application { configureTestApp(services) }

        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val report = client.post("/api/v1/reports") {
            bearerAuth(alice.accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                """{"type":"NUISANCE","title":"Graffiti","description":"On the bridge","category":"VANDALISM",""" +
                    """"latitude":51.5866,"longitude":4.7760}"""
            )
        }
        val reportId = Json.decodeFromString<ReportResponse>(report.bodyAsText()).id

        block(Setup(services, alice, bob, admin, reportId))
    }

    private suspend fun ApplicationTestBuilder.upload(
        reportId: String,
        token: String?,
        bytes: ByteArray? = ReportTestData.jpeg(),
        latitude: String? = null,
        longitude: String? = null
    ): HttpResponse =
        client.post("/api/v1/reports/$reportId/photos") {
            if (token != null) {
                bearerAuth(token)
            }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        if (latitude != null) append("latitude", latitude)
                        if (longitude != null) append("longitude", longitude)
                        if (bytes != null) {
                            append(
                                "photo",
                                bytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/jpeg")
                                    append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
                                }
                            )
                        }
                    }
                )
            )
        }

    private suspend fun HttpResponse.photo(): ReportPhotoResponse =
        Json.decodeFromString<ReportPhotoResponse>(bodyAsText())

    @Test
    fun `the owner uploads a photo and its GPS comes from the photo`() = testPhotos { setup ->
        val response = upload(setup.reportId, setup.alice.accessToken, ReportTestData.jpegWithGps(51.5856, 4.7763))

        assertEquals(HttpStatusCode.Created, response.status)
        val photo = response.photo()
        assertTrue(abs(assertNotNull(photo.latitude) - 51.5856) < 0.00001)
        assertEquals("PHOTO", photo.locationSource)
        assertEquals(setup.alice.user.id, photo.uploaderId)
        assertEquals(1, setup.services.photoStorage.files.size)
    }

    @Test
    fun `without GPS in the photo the app coordinates are used`() = testPhotos { setup ->
        val photo = upload(setup.reportId, setup.alice.accessToken, latitude = "51.59", longitude = "4.78").photo()

        assertEquals(51.59, photo.latitude)
        assertEquals("APP", photo.locationSource)
    }

    @Test
    fun `every user can list the photos and download the image`() = testPhotos { setup ->
        val bytes = ReportTestData.jpegWithGps(51.5856, 4.7763)
        val photo = upload(setup.reportId, setup.alice.accessToken, bytes).photo()

        val list = client.get("/api/v1/reports/${setup.reportId}/photos") { bearerAuth(setup.bob.accessToken) }
        val image = client.get(photo.url) { bearerAuth(setup.bob.accessToken) }

        val photos = Json.decodeFromString<List<ReportPhotoResponse>>(list.bodyAsText())
        assertEquals(listOf(photo.id), photos.map { it.id })
        assertEquals(HttpStatusCode.OK, image.status)
        assertEquals(ContentType.Image.JPEG, image.contentType()?.withoutParameters())
        assertTrue(bytes.contentEquals(image.bodyAsBytes()))
    }

    @Test
    fun `request without token returns 401`() = testPhotos { setup ->
        assertEquals(HttpStatusCode.Unauthorized, upload(setup.reportId, token = null).status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/reports/${setup.reportId}/photos").status)
    }

    @Test
    fun `someone else gets 403, an admin may add a photo`() = testPhotos { setup ->
        val asBob = upload(setup.reportId, setup.bob.accessToken)
        val asAdmin = upload(setup.reportId, setup.admin.accessToken)

        assertEquals(HttpStatusCode.Forbidden, asBob.status)
        assertEquals("FORBIDDEN", asBob.errorCode())
        assertEquals(HttpStatusCode.Created, asAdmin.status)
    }

    @Test
    fun `an unknown report gives 404 and an invalid id 400`() = testPhotos { setup ->
        val unknown = upload(UUID.randomUUID().toString(), setup.alice.accessToken)
        val invalid = client.get("/api/v1/reports/not-a-uuid/photos") { bearerAuth(setup.alice.accessToken) }

        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals("REPORT_NOT_FOUND", unknown.errorCode())
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
    }

    @Test
    fun `returns 400 without a photo or with only one coordinate`() = testPhotos { setup ->
        val noPhoto = upload(setup.reportId, setup.alice.accessToken, bytes = null)
        val oneCoordinate = upload(setup.reportId, setup.alice.accessToken, latitude = "51.59")

        assertEquals(HttpStatusCode.BadRequest, noPhoto.status)
        assertEquals("PHOTO_REQUIRED", noPhoto.errorCode())
        assertEquals(HttpStatusCode.BadRequest, oneCoordinate.status)
        assertEquals("INVALID_REQUEST", oneCoordinate.errorCode())
    }

    @Test
    fun `returns 413 for a photo over 5 MB and stores nothing`() = testPhotos { setup ->
        val tooLarge = ByteArray(PhotoRules.MAX_PHOTO_BYTES.toInt() + 1)

        val response = upload(setup.reportId, setup.alice.accessToken, tooLarge)

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertEquals("PHOTO_TOO_LARGE", response.errorCode())
        assertTrue(setup.services.photoStorage.files.isEmpty())
    }

    @Test
    fun `returns 415 for a file that is not a JPEG or PNG`() = testPhotos { setup ->
        val response = upload(setup.reportId, setup.alice.accessToken, "not an image".toByteArray())

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertEquals("UNSUPPORTED_PHOTO_TYPE", response.errorCode())
    }

    @Test
    fun `returns 409 for the fourth photo`() = testPhotos { setup ->
        repeat(3) { assertEquals(HttpStatusCode.Created, upload(setup.reportId, setup.alice.accessToken).status) }

        val response = upload(setup.reportId, setup.alice.accessToken)

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("PHOTO_LIMIT_REACHED", response.errorCode())
    }

    @Test
    fun `the uploader deletes a photo and then it is gone`() = testPhotos { setup ->
        val photo = upload(setup.reportId, setup.alice.accessToken).photo()

        val deleted = client.delete(photo.url) { bearerAuth(setup.alice.accessToken) }
        val image = client.get(photo.url) { bearerAuth(setup.alice.accessToken) }

        assertEquals(HttpStatusCode.NoContent, deleted.status)
        assertEquals(HttpStatusCode.NotFound, image.status)
        assertEquals("PHOTO_NOT_FOUND", image.errorCode())
        assertTrue(setup.services.photoStorage.files.isEmpty())
    }

    @Test
    fun `someone else cannot delete a photo, an admin can`() = testPhotos { setup ->
        val photo = upload(setup.reportId, setup.alice.accessToken).photo()

        val asBob = client.delete(photo.url) { bearerAuth(setup.bob.accessToken) }
        val asAdmin = client.delete(photo.url) { bearerAuth(setup.admin.accessToken) }

        assertEquals(HttpStatusCode.Forbidden, asBob.status)
        assertEquals(HttpStatusCode.NoContent, asAdmin.status)
    }

    @Test
    fun `deleting a report removes its photo files`() = testPhotos { setup ->
        upload(setup.reportId, setup.alice.accessToken)

        val response = client.delete("/api/v1/reports/${setup.reportId}") { bearerAuth(setup.alice.accessToken) }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertTrue(setup.services.photoStorage.files.isEmpty())
    }
}
