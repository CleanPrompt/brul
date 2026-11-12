package nl.brul.backend

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json
import nl.brul.backend.config.AppConfig
import nl.brul.backend.features.authentication.dto.AuthenticationResponse
import nl.brul.backend.features.authentication.service.AuthenticationService
import nl.brul.backend.features.authentication.service.JwtService
import nl.brul.backend.features.authentication.service.PasswordHasher
import nl.brul.backend.features.authorization.FakeAuthorizationRepository
import nl.brul.backend.features.authorization.service.AuthorizationSeeder
import nl.brul.backend.features.authorization.service.AuthorizationService
import nl.brul.backend.features.authorization.service.RoleService
import nl.brul.backend.features.feedback.FakeFeedbackRepository
import nl.brul.backend.features.feedback.service.FeedbackService
import nl.brul.backend.features.user.FakeUserRepository
import nl.brul.backend.features.user.service.UserService
import nl.brul.backend.features.vote.FakeVoteRepository
import nl.brul.backend.features.vote.service.VoteService
import nl.brul.backend.plugins.configureAuthentication
import nl.brul.backend.plugins.configureRateLimiting
import nl.brul.backend.plugins.configureRouting
import nl.brul.backend.plugins.configureSerialization
import nl.brul.backend.plugins.configureStatusPages
import nl.brul.backend.shared.error.ErrorResponse
import java.time.Instant

const val TEST_ADMIN_EMAIL = "admin@example.com"
/** Fixed creation time for users built directly in tests. */
val TEST_CREATED_AT: Instant = Instant.parse("2026-01-01T12:00:00Z")

const val TEST_PASSWORD = "Correct-password-123"

/** Configuration for tests; never reads `.env` or environment variables. */
fun testConfig(): AppConfig =
    AppConfig(
        host = "127.0.0.1",
        port = 0,
        databaseUrl = "jdbc:h2:mem:unused",
        databaseDriver = "org.h2.Driver",
        databaseUser = "sa",
        databasePassword = "",
        jwtSecret = "test-secret-that-is-only-used-in-tests",
        jwtIssuer = "test-issuer",
        jwtAudience = "test-audience",
        bootstrapAdminEmail = TEST_ADMIN_EMAIL
    )

/** All services wired with in-memory fakes, the same way `Application.module()` wires the real ones. */
class TestServices {
    val userRepository = FakeUserRepository()
    val authorizationRepository = FakeAuthorizationRepository().also { AuthorizationSeeder(it).seed() }
    val jwtService = JwtService(testConfig())
    val authorizationService = AuthorizationService(authorizationRepository, userRepository)
    val roleService = RoleService(authorizationRepository, userRepository)
    val userService = UserService(userRepository)
    val authenticationService = AuthenticationService(
        userRepository = userRepository,
        passwordHasher = PasswordHasher(),
        jwtService = jwtService,
        roleService = roleService,
        bootstrapAdminEmail = TEST_ADMIN_EMAIL
    )
    val feedbackRepository = FakeFeedbackRepository()
    val feedbackService = FeedbackService(feedbackRepository)
}

/** Installs the plugins and routes like `Application.module()`, but with [services]. */
fun Application.configureTestApp(services: TestServices) {
    configureSerialization()
    configureStatusPages()
    configureRateLimiting()
    configureAuthentication(services.jwtService, services.userRepository)
    configureRouting(
        services.authenticationService,
        services.userService,
        services.roleService,
        services.authorizationService,
        services.feedbackService
    )
}

suspend fun ApplicationTestBuilder.postJson(path: String, body: String): HttpResponse =
    client.post(path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

/** Registers a user through the API and returns the response with its token and id. */
suspend fun ApplicationTestBuilder.register(email: String, displayName: String = "Test User"): AuthenticationResponse {
    val response = postJson(
        "/api/v1/auth/register",
        """{"email":"$email","displayName":"$displayName","password":"$TEST_PASSWORD"}"""
    )
    return Json.decodeFromString<AuthenticationResponse>(response.bodyAsText())
}

suspend fun HttpResponse.errorCode(): String =
    Json.decodeFromString<ErrorResponse>(bodyAsText()).code
