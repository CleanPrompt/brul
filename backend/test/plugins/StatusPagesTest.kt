package nl.brul.backend.plugins

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.brul.backend.features.authentication.service.JwtService
import nl.brul.backend.features.user.FakeUserRepository
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.error.ErrorResponse
import nl.brul.backend.testConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class StatusPagesTest {
    private fun ApplicationTestBuilder.setUpApp() {
        application {
            configureSerialization()
            configureStatusPages()
            configureAuthentication(JwtService(testConfig()), FakeUserRepository())
            routing {
                get("/bad-request") { throw AppException.BadRequest("Invalid input") }
                get("/forbidden") { throw AppException.Forbidden("No access") }
                get("/not-found") { throw AppException.NotFound("User not found", "USER_NOT_FOUND") }
                get("/conflict") { throw AppException.Conflict("Already exists", "ALREADY_EXISTS") }
                get("/crash") { error("Unexpected failure") }
                post("/json") { call.respond(call.receive<ErrorResponse>()) }
                authenticate(JWT_AUTHENTICATION) {
                    get("/protected") { call.respond("secret") }
                }
            }
        }
    }

    private suspend fun HttpResponse.error(): ErrorResponse =
        Json.decodeFromString<ErrorResponse>(bodyAsText())

    private suspend fun ApplicationTestBuilder.assertError(path: String, status: HttpStatusCode, code: String) {
        val response = client.get(path)
        assertEquals(status, response.status, "status for $path")
        assertEquals(code, response.error().code, "code for $path")
    }

    @Test
    fun `application exceptions map to status codes`() = testApplication {
        setUpApp()

        assertError("/bad-request", HttpStatusCode.BadRequest, "INVALID_REQUEST")
        assertError("/forbidden", HttpStatusCode.Forbidden, "FORBIDDEN")
        assertError("/not-found", HttpStatusCode.NotFound, "USER_NOT_FOUND")
        assertError("/conflict", HttpStatusCode.Conflict, "ALREADY_EXISTS")
    }

    @Test
    fun `message is passed to the client`() = testApplication {
        setUpApp()

        assertEquals("Invalid input", client.get("/bad-request").error().message)
    }

    @Test
    fun `unexpected exception returns 500 without details`() = testApplication {
        setUpApp()

        val response = client.get("/crash")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals(ErrorResponse("INTERNAL_ERROR", "Internal server error"), response.error())
    }

    @Test
    fun `unknown route returns 404 in the error format`() = testApplication {
        setUpApp()

        assertError("/does-not-exist", HttpStatusCode.NotFound, "NOT_FOUND")
    }

    @Test
    fun `protected route without token returns 401 in the error format`() = testApplication {
        setUpApp()

        assertError("/protected", HttpStatusCode.Unauthorized, "UNAUTHORIZED")
    }

    @Test
    fun `body without content type returns 415`() = testApplication {
        setUpApp()

        val response = client.post("/json") { setBody(VALID_JSON) }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertEquals("UNSUPPORTED_MEDIA_TYPE", response.error().code)
    }

    @Test
    fun `body that is not json returns 415`() = testApplication {
        setUpApp()

        val response = client.post("/json") {
            contentType(ContentType.Text.Plain)
            setBody(VALID_JSON)
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertEquals("UNSUPPORTED_MEDIA_TYPE", response.error().code)
    }

    @Test
    fun `malformed json still returns 400`() = testApplication {
        setUpApp()

        val response = client.post("/json") {
            contentType(ContentType.Application.Json)
            setBody("""{"code":""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("INVALID_REQUEST", response.error().code)
    }

    @Test
    fun `an unknown json field returns 400`() = testApplication {
        setUpApp()

        val response = client.post("/json") {
            contentType(ContentType.Application.Json)
            setBody("""{"code":"X","message":"Y","extra":true}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("INVALID_REQUEST", response.error().code)
    }

    @Test
    fun `wrong method on an existing path returns 405 in the error format`() = testApplication {
        setUpApp()

        val response = client.put("/crash")

        assertEquals(HttpStatusCode.MethodNotAllowed, response.status)
        assertEquals("METHOD_NOT_ALLOWED", response.error().code)
    }

    private companion object {
        const val VALID_JSON = """{"code":"X","message":"Y"}"""
    }
}
