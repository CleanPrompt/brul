package nl.brul.backend.features.user

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import nl.brul.backend.TEST_ADMIN_EMAIL
import nl.brul.backend.TestServices
import nl.brul.backend.configureTestApp
import nl.brul.backend.errorCode
import nl.brul.backend.register
import nl.brul.backend.shared.image.ImageType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ProfileImageRoutesTest {
    // Not real photos: only the first bytes matter.
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x01)
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x02)

    private fun path(userId: String) = "/api/v1/users/$userId/profile-image"

    /** Sends [bytes] as the raw body, the way the app uploads a photo. */
    private suspend fun ApplicationTestBuilder.upload(userId: String, token: String, bytes: ByteArray): HttpResponse =
        client.put(path(userId)) {
            bearerAuth(token)
            contentType(ContentType.Image.PNG)
            setBody(bytes)
        }

    @Test
    fun `the owner uploads an image and every logged-in user can see it`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")

        val uploaded = upload(alice.user.id, alice.accessToken, png)
        val asBob = client.get(path(alice.user.id)) { bearerAuth(bob.accessToken) }

        assertEquals(HttpStatusCode.NoContent, uploaded.status)
        assertEquals(HttpStatusCode.OK, asBob.status)
        assertEquals("image/png", asBob.headers[HttpHeaders.ContentType])
        assertContentEquals(png, asBob.readRawBytes())
    }

    @Test
    fun `a new upload replaces the old image and gets its own content type`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        upload(alice.user.id, alice.accessToken, png)
        upload(alice.user.id, alice.accessToken, jpeg)
        val response = client.get(path(alice.user.id)) { bearerAuth(alice.accessToken) }

        assertEquals("image/jpeg", response.headers[HttpHeaders.ContentType])
        assertContentEquals(jpeg, response.readRawBytes())
    }

    @Test
    fun `the owner deletes the image, then it is gone`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        upload(alice.user.id, alice.accessToken, png)

        val deleted = client.delete(path(alice.user.id)) { bearerAuth(alice.accessToken) }
        val afterwards = client.get(path(alice.user.id)) { bearerAuth(alice.accessToken) }
        val again = client.delete(path(alice.user.id)) { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.NoContent, deleted.status)
        assertEquals(HttpStatusCode.NotFound, afterwards.status)
        assertEquals("PROFILE_IMAGE_NOT_FOUND", afterwards.errorCode())
        assertEquals(HttpStatusCode.NotFound, again.status)
    }

    @Test
    fun `returns 400 for a file that is not a JPEG or PNG`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = upload(alice.user.id, alice.accessToken, "<svg></svg>".toByteArray())

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("INVALID_IMAGE_TYPE", response.errorCode())
    }

    @Test
    fun `returns 400 for an image over 2 MB`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = upload(alice.user.id, alice.accessToken, png.copyOf(ImageType.MAX_BYTES + 1))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("IMAGE_TOO_LARGE", response.errorCode())
    }

    @Test
    fun `returns 400 for an invalid user id`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = client.get(path("not-a-uuid")) { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `returns 401 without a token`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        assertEquals(HttpStatusCode.Unauthorized, client.put(path(alice.user.id)) { setBody(png) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get(path(alice.user.id)).status)
        assertEquals(HttpStatusCode.Unauthorized, client.delete(path(alice.user.id)).status)
    }

    @Test
    fun `another user gets 403, an admin may change and delete the image`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        upload(alice.user.id, alice.accessToken, png)

        val uploadAsBob = upload(alice.user.id, bob.accessToken, jpeg)
        val deleteAsBob = client.delete(path(alice.user.id)) { bearerAuth(bob.accessToken) }
        val uploadAsAdmin = upload(alice.user.id, admin.accessToken, jpeg)
        val deleteAsAdmin = client.delete(path(alice.user.id)) { bearerAuth(admin.accessToken) }

        assertEquals(HttpStatusCode.Forbidden, uploadAsBob.status)
        assertEquals(HttpStatusCode.Forbidden, deleteAsBob.status)
        assertEquals(HttpStatusCode.NoContent, uploadAsAdmin.status)
        assertEquals(HttpStatusCode.NoContent, deleteAsAdmin.status)
    }

    @Test
    fun `returns 404 for an unknown user`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val unknown = UUID.randomUUID().toString()

        val uploadResponse = upload(unknown, admin.accessToken, png)
        val getResponse = client.get(path(unknown)) { bearerAuth(admin.accessToken) }

        assertEquals(HttpStatusCode.NotFound, uploadResponse.status)
        assertEquals("USER_NOT_FOUND", uploadResponse.errorCode())
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
        assertEquals("PROFILE_IMAGE_NOT_FOUND", getResponse.errorCode())
    }
}
