package nl.example.backend.features.authorization

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
import nl.example.backend.features.authorization.dto.RoleResponse
import nl.example.backend.features.authorization.model.DefaultRole
import nl.example.backend.register
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class RoleRoutesTest {
    private val services = TestServices()
    private val adminRoleId: String =
        services.authorizationRepository.findRoleByName(DefaultRole.ADMIN.name)!!.id.toString()

    private suspend fun ApplicationTestBuilder.assignRole(token: String, userId: String, roleId: String) =
        client.post("/api/v1/users/$userId/roles") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"roleId":"$roleId"}""")
        }

    @Test
    fun `admin can list the roles`() = testApplication {
        application { configureTestApp(services) }
        val admin = register(TEST_ADMIN_EMAIL)

        val response = client.get("/api/v1/roles") { bearerAuth(admin.accessToken) }

        assertEquals(HttpStatusCode.OK, response.status)
        val names = Json.decodeFromString<List<RoleResponse>>(response.bodyAsText()).map { it.name }.toSet()
        assertEquals(setOf("ADMIN", "USER"), names)
    }

    @Test
    fun `regular user cannot list or assign roles`() = testApplication {
        application { configureTestApp(services) }
        val alice = register("alice@example.com")

        val list = client.get("/api/v1/roles") { bearerAuth(alice.accessToken) }
        val assign = assignRole(alice.accessToken, alice.user.id, adminRoleId)

        assertEquals(HttpStatusCode.Forbidden, list.status)
        assertEquals(HttpStatusCode.Forbidden, assign.status)
    }

    @Test
    fun `permissions change immediately, also for an existing token`() = testApplication {
        application { configureTestApp(services) }
        val admin = register(TEST_ADMIN_EMAIL)
        val alice = register("alice@example.com")

        // Alice keeps the same token the whole time.
        val before = client.get("/api/v1/users") { bearerAuth(alice.accessToken) }
        assignRole(admin.accessToken, alice.user.id, adminRoleId)
        val afterAssign = client.get("/api/v1/users") { bearerAuth(alice.accessToken) }
        client.delete("/api/v1/users/${alice.user.id}/roles/$adminRoleId") { bearerAuth(admin.accessToken) }
        val afterRemove = client.get("/api/v1/users") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.Forbidden, before.status)
        assertEquals(HttpStatusCode.OK, afterAssign.status)
        assertEquals(HttpStatusCode.Forbidden, afterRemove.status)
    }

    @Test
    fun `assigning an unknown role returns 404`() = testApplication {
        application { configureTestApp(services) }
        val admin = register(TEST_ADMIN_EMAIL)

        val response = assignRole(admin.accessToken, admin.user.id, UUID.randomUUID().toString())

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("ROLE_NOT_FOUND", response.errorCode())
    }

    @Test
    fun `admin cannot remove own ADMIN role`() = testApplication {
        application { configureTestApp(services) }
        val admin = register(TEST_ADMIN_EMAIL)

        val response = client.delete("/api/v1/users/${admin.user.id}/roles/$adminRoleId") {
            bearerAuth(admin.accessToken)
        }
        val afterwards = client.get("/api/v1/roles") { bearerAuth(admin.accessToken) }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("INVALID_REQUEST", response.errorCode())
        assertEquals(HttpStatusCode.OK, afterwards.status)
    }

    @Test
    fun `user can see own roles`() = testApplication {
        application { configureTestApp(services) }
        val alice = register("alice@example.com")

        val response = client.get("/api/v1/users/${alice.user.id}/roles") { bearerAuth(alice.accessToken) }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(listOf("USER"), Json.decodeFromString<List<RoleResponse>>(response.bodyAsText()).map { it.name })
    }
}
