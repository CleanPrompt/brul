package nl.example.backend.features.feedback

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.example.backend.TEST_ADMIN_EMAIL
import nl.example.backend.TestServices
import nl.example.backend.configureTestApp
import nl.example.backend.errorCode
import nl.example.backend.features.feedback.dto.FeedbackResponse
import nl.example.backend.register
import nl.example.backend.shared.paging.PageResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedbackRoutesTest {

    private suspend fun ApplicationTestBuilder.createFeedback(
        token: String,
        message: String = "The app is slow"
    ): HttpResponse =
        client.post("/api/v1/feedback") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"message":"$message"}""")
        }

    /** Creates an item and returns its id. */
    private suspend fun ApplicationTestBuilder.createFeedbackId(token: String): String =
        createFeedback(token).body().id

    private suspend fun ApplicationTestBuilder.patchJson(path: String, token: String, body: String): HttpResponse =
        client.patch(path) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun HttpResponse.body(): FeedbackResponse =
        Json.decodeFromString<FeedbackResponse>(bodyAsText())

    private suspend fun HttpResponse.page(): PageResponse<FeedbackResponse> =
        Json.decodeFromString<PageResponse<FeedbackResponse>>(bodyAsText())

    @Test
    fun `request without token returns 401`() = testApplication {
        application { configureTestApp(TestServices()) }

        val list = client.get("/api/v1/feedback")
        val create = client.post("/api/v1/feedback")

        assertEquals(HttpStatusCode.Unauthorized, list.status)
        assertEquals(HttpStatusCode.Unauthorized, create.status)
    }

    @Test
    fun `user creates feedback and becomes the owner`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = createFeedback(alice.accessToken)

        assertEquals(HttpStatusCode.Created, response.status)
        val feedback = response.body()
        assertEquals(alice.user.id, feedback.userId)
        assertEquals("OPEN", feedback.status)
    }

    @Test
    fun `create with an empty message or a malformed body returns 400`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val empty = createFeedback(alice.accessToken, message = "   ")
        val malformed = client.post("/api/v1/feedback") {
            bearerAuth(alice.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{"text":"wrong field"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, empty.status)
        assertEquals(HttpStatusCode.BadRequest, malformed.status)
    }

    @Test
    fun `user lists only own feedback and admin lists all`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        createFeedback(alice.accessToken)
        createFeedback(bob.accessToken)

        val asAlice = client.get("/api/v1/feedback") { bearerAuth(alice.accessToken) }
        val asAdmin = client.get("/api/v1/feedback") { bearerAuth(admin.accessToken) }

        assertEquals(listOf(alice.user.id), asAlice.page().items.map { it.userId })
        assertEquals(2, asAdmin.page().total)
    }

    @Test
    fun `userId filter cannot widen a regular user's search`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        createFeedback(alice.accessToken)
        createFeedback(bob.accessToken)

        val response = client.get("/api/v1/feedback?userId=${bob.user.id}") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf(alice.user.id), response.page().items.map { it.userId })
    }

    @Test
    fun `admin filters on owner, status and text`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        createFeedback(alice.accessToken, message = "The app is slow")
        val crashId = createFeedback(bob.accessToken, message = "Crash on start").body().id
        patchJson("/api/v1/feedback/$crashId/status", admin.accessToken, """{"status":"CLOSED"}""")

        val byOwner = client.get("/api/v1/feedback?userId=${alice.user.id}") { bearerAuth(admin.accessToken) }
        val byStatus = client.get("/api/v1/feedback?status=CLOSED") { bearerAuth(admin.accessToken) }
        val byText = client.get("/api/v1/feedback?q=SLOW") { bearerAuth(admin.accessToken) }

        assertEquals(listOf(alice.user.id), byOwner.page().items.map { it.userId })
        assertEquals(listOf(crashId), byStatus.page().items.map { it.id })
        assertEquals(listOf(alice.user.id), byText.page().items.map { it.userId })
    }

    @Test
    fun `feedback list sorts on status and pages with limit and offset`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val openId = createFeedbackId(admin.accessToken)
        val closedId = createFeedbackId(admin.accessToken)
        val inProgressId = createFeedbackId(admin.accessToken)
        patchJson("/api/v1/feedback/$closedId/status", admin.accessToken, """{"status":"CLOSED"}""")
        patchJson("/api/v1/feedback/$inProgressId/status", admin.accessToken, """{"status":"IN_PROGRESS"}""")

        val sorted = client.get("/api/v1/feedback?sort=status&order=asc") { bearerAuth(admin.accessToken) }
        val secondPage = client.get("/api/v1/feedback?sort=status&order=asc&limit=2&offset=2") {
            bearerAuth(admin.accessToken)
        }

        assertEquals(listOf(closedId, inProgressId, openId), sorted.page().items.map { it.id })
        assertEquals(listOf(openId), secondPage.page().items.map { it.id })
        assertEquals(3, secondPage.page().total)
    }

    @Test
    fun `invalid feedback search parameters return 400`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val paths = listOf(
            "/api/v1/feedback?status=DONE",
            "/api/v1/feedback?sort=message",
            "/api/v1/feedback?userId=not-a-uuid",
            "/api/v1/feedback?limit=500"
        )

        paths.forEach { path ->
            val response = client.get(path) { bearerAuth(alice.accessToken) }
            assertEquals(HttpStatusCode.BadRequest, response.status, path)
        }
    }

    @Test
    fun `owner and admin can read an item, another user cannot`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val id = createFeedbackId(alice.accessToken)

        val asOwner = client.get("/api/v1/feedback/$id") { bearerAuth(alice.accessToken) }
        val asAdmin = client.get("/api/v1/feedback/$id") { bearerAuth(admin.accessToken) }
        val asOther = client.get("/api/v1/feedback/$id") { bearerAuth(bob.accessToken) }

        assertEquals(HttpStatusCode.OK, asOwner.status)
        assertEquals(HttpStatusCode.OK, asAdmin.status)
        assertEquals(HttpStatusCode.Forbidden, asOther.status)
    }

    @Test
    fun `unknown id returns 404 and an invalid id returns 400`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val unknown = client.get("/api/v1/feedback/${UUID.randomUUID()}") { bearerAuth(alice.accessToken) }
        val invalid = client.get("/api/v1/feedback/not-a-uuid") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals("FEEDBACK_NOT_FOUND", unknown.errorCode())
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
    }

    @Test
    fun `owner can edit the message, another user cannot`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val id = createFeedbackId(alice.accessToken)

        val asOwner = patchJson("/api/v1/feedback/$id", alice.accessToken, """{"message":"Updated"}""")
        val asOther = patchJson("/api/v1/feedback/$id", bob.accessToken, """{"message":"Hijacked"}""")

        assertEquals(HttpStatusCode.OK, asOwner.status)
        assertEquals("Updated", asOwner.body().message)
        assertEquals(HttpStatusCode.Forbidden, asOther.status)
    }

    @Test
    fun `admin can change the status, the owner cannot`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")
        val id = createFeedbackId(alice.accessToken)

        val asOwner = patchJson("/api/v1/feedback/$id/status", alice.accessToken, """{"status":"CLOSED"}""")
        val asAdmin = patchJson("/api/v1/feedback/$id/status", admin.accessToken, """{"status":"IN_PROGRESS"}""")

        assertEquals(HttpStatusCode.Forbidden, asOwner.status)
        assertEquals(HttpStatusCode.OK, asAdmin.status)
        assertEquals("IN_PROGRESS", asAdmin.body().status)
    }

    @Test
    fun `unknown status returns 400 and unknown item returns 404`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val id = createFeedbackId(admin.accessToken)

        val badStatus = patchJson("/api/v1/feedback/$id/status", admin.accessToken, """{"status":"DONE"}""")
        val unknown = patchJson(
            "/api/v1/feedback/${UUID.randomUUID()}/status", admin.accessToken, """{"status":"CLOSED"}"""
        )

        assertEquals(HttpStatusCode.BadRequest, badStatus.status)
        assertEquals(HttpStatusCode.NotFound, unknown.status)
    }

    @Test
    fun `owner can delete an item, after which it is gone`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val id = createFeedbackId(alice.accessToken)

        val delete = client.delete("/api/v1/feedback/$id") { bearerAuth(alice.accessToken) }
        val afterwards = client.get("/api/v1/feedback/$id") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.NoContent, delete.status)
        assertEquals(HttpStatusCode.NotFound, afterwards.status)
    }

    @Test
    fun `another user cannot delete an item but admin can`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val id = createFeedbackId(alice.accessToken)

        val asOther = client.delete("/api/v1/feedback/$id") { bearerAuth(bob.accessToken) }
        val asAdmin = client.delete("/api/v1/feedback/$id") { bearerAuth(admin.accessToken) }

        assertEquals(HttpStatusCode.Forbidden, asOther.status)
        assertEquals(HttpStatusCode.NoContent, asAdmin.status)
    }
}
