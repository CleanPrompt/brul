package nl.example.backend.features.user

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.example.backend.TEST_ADMIN_EMAIL
import nl.example.backend.TEST_PASSWORD
import nl.example.backend.TestServices
import nl.example.backend.configureTestApp
import nl.example.backend.errorCode
import nl.example.backend.features.user.dto.UserResponse
import nl.example.backend.postJson
import nl.example.backend.register
import nl.example.backend.shared.paging.PageResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRoutesTest {

    @Test
    fun `request without token returns 401`() = testApplication {
        application { configureTestApp(TestServices()) }

        val response = client.get("/api/v1/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("UNAUTHORIZED", response.errorCode())
    }

    @Test
    fun `me returns the current user`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com", "Alice")

        val response = client.get("/api/v1/users/me") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Alice", Json.decodeFromString<UserResponse>(response.bodyAsText()).displayName)
    }

    @Test
    fun `regular user cannot list all users`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = client.get("/api/v1/users") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `admin can list all users`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        register("alice@example.com")

        val response = client.get("/api/v1/users") { bearerAuth(admin.accessToken) }

        assertEquals(HttpStatusCode.OK, response.status)
        val page = Json.decodeFromString<PageResponse<UserResponse>>(response.bodyAsText())
        assertEquals(2, page.items.size)
        assertEquals(2, page.total)
    }

    @Test
    fun `admin searches users by text and active, sorted by display name`() = testApplication {
        val services = TestServices()
        application { configureTestApp(services) }
        val admin = register(TEST_ADMIN_EMAIL, "Admin")
        register("alice@example.com", "Alice")
        val bob = register("bob@example.com", "Bob")
        register("carol@example.org", "Carol")
        client.delete("/api/v1/users/${bob.user.id}") { bearerAuth(admin.accessToken) }

        val byText = client.get("/api/v1/users?q=EXAMPLE.COM&sort=displayName&order=asc") {
            bearerAuth(admin.accessToken)
        }
        val inactive = client.get("/api/v1/users?active=false") { bearerAuth(admin.accessToken) }

        val names = Json.decodeFromString<PageResponse<UserResponse>>(byText.bodyAsText()).items.map { it.displayName }
        assertEquals(listOf("Admin", "Alice", "Bob"), names)
        val inactiveIds = Json.decodeFromString<PageResponse<UserResponse>>(inactive.bodyAsText()).items.map { it.id }
        assertEquals(listOf(bob.user.id), inactiveIds)
    }

    @Test
    fun `user list pages with limit and offset`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        register("alice@example.com")
        register("bob@example.com")

        val response = client.get("/api/v1/users?limit=2&offset=2") { bearerAuth(admin.accessToken) }

        val page = Json.decodeFromString<PageResponse<UserResponse>>(response.bodyAsText())
        assertEquals(1, page.items.size)
        assertEquals(3, page.total)
        assertEquals(2, page.limit)
        assertEquals(2, page.offset)
    }

    @Test
    fun `invalid search parameters return 400`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)

        val paths = listOf(
            "/api/v1/users?limit=0",
            "/api/v1/users?limit=101",
            "/api/v1/users?offset=-1",
            "/api/v1/users?limit=ten",
            "/api/v1/users?active=yes",
            "/api/v1/users?sort=password",
            "/api/v1/users?order=up",
            "/api/v1/users?q=${"x".repeat(101)}"
        )

        paths.forEach { path ->
            val response = client.get(path) { bearerAuth(admin.accessToken) }
            assertEquals(HttpStatusCode.BadRequest, response.status, path)
        }
    }

    @Test
    fun `regular user cannot search users`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")

        val response = client.get("/api/v1/users?q=admin") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `user can read own account but not another account`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")

        val own = client.get("/api/v1/users/${alice.user.id}") { bearerAuth(alice.accessToken) }
        val other = client.get("/api/v1/users/${bob.user.id}") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.OK, own.status)
        assertEquals(HttpStatusCode.Forbidden, other.status)
    }

    @Test
    fun `admin gets 404 for an unknown user and 400 for an invalid id`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)

        val unknown = client.get("/api/v1/users/${UUID.randomUUID()}") { bearerAuth(admin.accessToken) }
        val invalid = client.get("/api/v1/users/not-a-uuid") { bearerAuth(admin.accessToken) }

        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals("USER_NOT_FOUND", unknown.errorCode())
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
    }

    @Test
    fun `user can update own display name`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com", "Alice")

        val response = client.patch("/api/v1/users/${alice.user.id}") {
            bearerAuth(alice.accessToken)
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Alicia"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Alicia", Json.decodeFromString<UserResponse>(response.bodyAsText()).displayName)
    }

    @Test
    fun `regular user cannot deactivate another user`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")

        val response = client.delete("/api/v1/users/${bob.user.id}") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `deactivated user's token stops working`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")

        val deactivate = client.delete("/api/v1/users/${alice.user.id}") { bearerAuth(admin.accessToken) }
        val afterwards = client.get("/api/v1/users/me") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.NoContent, deactivate.status)
        assertEquals(HttpStatusCode.Unauthorized, afterwards.status)
    }

    @Test
    fun `reactivated user can log in again but the old token stays invalid`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")
        client.delete("/api/v1/users/${alice.user.id}") { bearerAuth(admin.accessToken) }

        val reactivate = client.post("/api/v1/users/${alice.user.id}/reactivate") { bearerAuth(admin.accessToken) }
        val oldToken = client.get("/api/v1/users/me") { bearerAuth(alice.accessToken) }
        val login = postJson(
            "/api/v1/auth/login",
            """{"email":"alice@example.com","password":"$TEST_PASSWORD"}"""
        )

        assertEquals(HttpStatusCode.NoContent, reactivate.status)
        assertEquals(HttpStatusCode.Unauthorized, oldToken.status)
        assertEquals(HttpStatusCode.OK, login.status)
    }

    @Test
    fun `regular user cannot reactivate a user`() = testApplication {
        application { configureTestApp(TestServices()) }
        val alice = register("alice@example.com")
        val bob = register("bob@example.com")

        val response = client.post("/api/v1/users/${bob.user.id}/reactivate") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `reactivate without token returns 401`() = testApplication {
        application { configureTestApp(TestServices()) }

        val response = client.post("/api/v1/users/${UUID.randomUUID()}/reactivate")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `reactivate returns 404 for an unknown user and 400 for an invalid id`() = testApplication {
        application { configureTestApp(TestServices()) }
        val admin = register(TEST_ADMIN_EMAIL)

        val unknown = client.post("/api/v1/users/${UUID.randomUUID()}/reactivate") { bearerAuth(admin.accessToken) }
        val invalid = client.post("/api/v1/users/not-a-uuid/reactivate") { bearerAuth(admin.accessToken) }

        assertEquals(HttpStatusCode.NotFound, unknown.status)
        assertEquals("USER_NOT_FOUND", unknown.errorCode())
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
    }
}
