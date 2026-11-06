package nl.example.backend.features.authentication

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
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
import nl.example.backend.TEST_PASSWORD
import nl.example.backend.TestServices
import nl.example.backend.configureTestApp
import nl.example.backend.errorCode
import nl.example.backend.features.authentication.dto.AuthenticationResponse
import nl.example.backend.postJson
import nl.example.backend.register
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthenticationRoutesTest {
    private val registerBody =
        """{"email":"alice@example.com","displayName":"Alice","password":"$TEST_PASSWORD"}"""

    @Test
    fun `register returns 201`() = testApplication {
        application { configureTestApp(TestServices()) }

        assertEquals(HttpStatusCode.Created, postJson("/api/v1/auth/register", registerBody).status)
    }

    @Test
    fun `register with an existing email returns 409`() = testApplication {
        application { configureTestApp(TestServices()) }
        postJson("/api/v1/auth/register", registerBody)

        val response = postJson("/api/v1/auth/register", registerBody)

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("EMAIL_ALREADY_REGISTERED", response.errorCode())
    }

    @Test
    fun `login with a wrong password returns 401`() = testApplication {
        application { configureTestApp(TestServices()) }
        register("alice@example.com")

        val response = postJson(
            "/api/v1/auth/login",
            """{"email":"alice@example.com","password":"Wrong-password-123"}"""
        )

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("INVALID_CREDENTIALS", response.errorCode())
    }

    @Test
    fun `malformed body returns 400`() = testApplication {
        application { configureTestApp(TestServices()) }

        val response = postJson("/api/v1/auth/login", """{"email":""")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("INVALID_REQUEST", response.errorCode())
    }

    @Test
    fun `too many authentication requests return 429`() = testApplication {
        application { configureTestApp(TestServices()) }
        val loginBody = """{"email":"nobody@example.com","password":"Wrong-password-123"}"""

        repeat(10) { postJson("/api/v1/auth/login", loginBody) }
        val response = postJson("/api/v1/auth/login", loginBody)

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals("TOO_MANY_REQUESTS", response.errorCode())
    }

    @Test
    fun `logout returns 204 and the token stops working`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val logout = client.post("/api/v1/auth/logout") { bearerAuth(alice.accessToken) }
        val afterwards = client.get("/api/v1/users/me") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.NoContent, logout.status)
        assertEquals(HttpStatusCode.Unauthorized, afterwards.status)
    }

    @Test
    fun `logging in again after logout gives a working token`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        client.post("/api/v1/auth/logout") { bearerAuth(alice.accessToken) }

        val login = postJson(
            "/api/v1/auth/login",
            """{"email":"alice@example.com","password":"$TEST_PASSWORD"}"""
        )
        val newToken = Json.decodeFromString<AuthenticationResponse>(login.bodyAsText()).accessToken
        val me = client.get("/api/v1/users/me") { bearerAuth(newToken) }

        assertEquals(HttpStatusCode.OK, me.status)
    }

    @Test
    fun `logout without token returns 401`() = testApplication {
        application { configureTestApp(TestServices()) }

        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/v1/auth/logout").status)
    }

    @Test
    fun `change password returns a new token and the old token stops working`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = changePassword(alice.accessToken, currentPassword = TEST_PASSWORD)
        val newToken = Json.decodeFromString<AuthenticationResponse>(response.bodyAsText()).accessToken
        val oldTokenMe = client.get("/api/v1/users/me") { bearerAuth(alice.accessToken) }
        val newTokenMe = client.get("/api/v1/users/me") { bearerAuth(newToken) }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(HttpStatusCode.Unauthorized, oldTokenMe.status)
        assertEquals(HttpStatusCode.OK, newTokenMe.status)
    }

    @Test
    fun `change password with a wrong current password returns 400`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = changePassword(alice.accessToken, currentPassword = "Wrong-password-123")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("INVALID_CURRENT_PASSWORD", response.errorCode())
    }

    @Test
    fun `change password without token returns 401`() = testApplication {
        application { configureTestApp(TestServices()) }

        val response = postJson(
            "/api/v1/users/me/password",
            """{"currentPassword":"$TEST_PASSWORD","newPassword":"$NEW_PASSWORD"}"""
        )

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `too many password changes return 429`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        repeat(5) { changePassword(alice.accessToken, currentPassword = "Wrong-password-123") }
        val response = changePassword(alice.accessToken, currentPassword = "Wrong-password-123")

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals("TOO_MANY_REQUESTS", response.errorCode())
    }

    private suspend fun ApplicationTestBuilder.changePassword(token: String, currentPassword: String): HttpResponse =
        client.post("/api/v1/users/me/password") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"currentPassword":"$currentPassword","newPassword":"$NEW_PASSWORD"}""")
        }

    private companion object {
        const val NEW_PASSWORD = "New-password-456"
    }
}
