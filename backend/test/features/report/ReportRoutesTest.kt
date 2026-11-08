package nl.brul.backend.features.report

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.brul.backend.TEST_ADMIN_EMAIL
import nl.brul.backend.TestServices
import nl.brul.backend.configureTestApp
import nl.brul.backend.errorCode
import nl.brul.backend.features.report.dto.ReportResponse
import nl.brul.backend.register
import nl.brul.backend.shared.paging.PageResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportRoutesTest {
    private val bikeStolen =
        """{"type":"SAFETY","title":"Bike stolen","description":"At the station","category":"THEFT",""" +
            """"latitude":51.5866,"longitude":4.7760,"urgent":true}"""

    private fun update(status: String) =
        """{"type":"SAFETY","title":"Bike found","description":"It is back","category":"THEFT",""" +
            """"latitude":51.5866,"longitude":4.7760,"status":"$status","urgent":false}"""

    private suspend fun ApplicationTestBuilder.createReport(token: String, body: String = bikeStolen): HttpResponse =
        client.post("/api/v1/reports") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun ApplicationTestBuilder.updateReport(id: String, token: String, body: String): HttpResponse =
        client.put("/api/v1/reports/$id") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun ApplicationTestBuilder.createReportId(token: String, body: String = bikeStolen): String =
        createReport(token, body).body().id

    private suspend fun HttpResponse.body(): ReportResponse =
        Json.decodeFromString<ReportResponse>(bodyAsText())

    private suspend fun HttpResponse.page(): PageResponse<ReportResponse> =
        Json.decodeFromString<PageResponse<ReportResponse>>(bodyAsText())

    @Test
    fun `request without token returns 401`() = testApplication {
        application { configureTestApp(TestServices()) }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/reports").status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/reports").status)
    }

    @Test
    fun `user creates a report and becomes the owner`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = createReport(alice.accessToken)

        assertEquals(HttpStatusCode.Created, response.status)
        val report = response.body()
        assertEquals(alice.user.id, report.ownerId)
        assertEquals("OPEN", report.status)
        assertEquals(true, report.urgent)
    }

    @Test
    fun `create with a wrong category or a malformed body returns 400`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val wrongCategory = createReport(alice.accessToken, bikeStolen.replace("THEFT", "NOISE"))
        val malformed = createReport(alice.accessToken, """{"title":"Only a title"}""")

        assertEquals(HttpStatusCode.BadRequest, wrongCategory.status)
        assertEquals("INVALID_REQUEST", wrongCategory.errorCode())
        assertEquals(HttpStatusCode.BadRequest, malformed.status)
    }

    @Test
    fun `every user sees all reports, filtered and paged`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        createReport(alice.accessToken)
        createReport(alice.accessToken)
        createReport(
            alice.accessToken,
            """{"type":"NUISANCE","title":"Loud music","description":"Every night","category":"NOISE",""" +
                """"latitude":51.5555,"longitude":5.0913}"""
        )

        val all = client.get("/api/v1/reports?limit=2") { bearerAuth(bob.accessToken) }.page()
        val nuisance = client.get("/api/v1/reports?type=NUISANCE") { bearerAuth(bob.accessToken) }.page()
        val nearBreda = client.get("/api/v1/reports?latitude=51.5866&longitude=4.7760&radiusKm=5") {
            bearerAuth(bob.accessToken)
        }.page()

        assertEquals(2, all.items.size)
        assertEquals(3, all.total)
        assertEquals(listOf("Loud music"), nuisance.items.map { it.title })
        assertEquals(2, nearBreda.total)
    }

    @Test
    fun `an area without a location returns 400`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = client.get("/api/v1/reports?radiusKm=5") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `opening a report counts a view, an unknown or invalid id gives 404 or 400`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val id = createReportId(alice.accessToken)

        client.get("/api/v1/reports/$id") { bearerAuth(bob.accessToken) }
        val second = client.get("/api/v1/reports/$id") { bearerAuth(bob.accessToken) }
        val unknown = client.get("/api/v1/reports/${UUID.randomUUID()}") { bearerAuth(bob.accessToken) }
        val invalid = client.get("/api/v1/reports/not-a-uuid") { bearerAuth(bob.accessToken) }

        assertEquals(2, second.body().viewCount)
        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals("REPORT_NOT_FOUND", unknown.errorCode())
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
    }

    @Test
    fun `the owner and an admin may update, someone else gets 403`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val id = createReportId(alice.accessToken)

        val asBob = updateReport(id, bob.accessToken, update("IN_PROGRESS"))
        val asAlice = updateReport(id, alice.accessToken, update("IN_PROGRESS"))
        val asAdmin = updateReport(id, admin.accessToken, update("RESOLVED"))

        assertEquals(HttpStatusCode.Forbidden, asBob.status)
        assertEquals(HttpStatusCode.OK, asAlice.status)
        assertEquals("Bike found", asAlice.body().title)
        assertEquals(alice.user.id, asAlice.body().ownerId)
        assertEquals("RESOLVED", asAdmin.body().status)
    }

    @Test
    fun `a resolved report cannot go back, which returns 409`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val id = createReportId(alice.accessToken)
        updateReport(id, alice.accessToken, update("RESOLVED"))

        val response = updateReport(id, alice.accessToken, update("OPEN"))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("INVALID_STATUS_CHANGE", response.errorCode())
    }

    @Test
    fun `the owner and an admin may delete, someone else gets 403`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")
        val first = createReportId(alice.accessToken)
        val second = createReportId(alice.accessToken)

        val asBob = client.delete("/api/v1/reports/$first") { bearerAuth(bob.accessToken) }
        val asAlice = client.delete("/api/v1/reports/$first") { bearerAuth(alice.accessToken) }
        val again = client.delete("/api/v1/reports/$first") { bearerAuth(alice.accessToken) }
        val asAdmin = client.delete("/api/v1/reports/$second") { bearerAuth(admin.accessToken) }

        assertEquals(HttpStatusCode.Forbidden, asBob.status)
        assertEquals(HttpStatusCode.NoContent, asAlice.status)
        assertEquals(HttpStatusCode.NotFound, again.status)
        assertEquals(HttpStatusCode.NoContent, asAdmin.status)
    }
}
