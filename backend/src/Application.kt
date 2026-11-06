package nl.example.backend

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import nl.example.backend.config.AppConfig
import nl.example.backend.config.DatabaseFactory
import nl.example.backend.features.authentication.service.AuthenticationService
import nl.example.backend.features.authentication.service.JwtService
import nl.example.backend.features.authentication.service.PasswordHasher
import nl.example.backend.features.authorization.repository.ExposedAuthorizationRepository
import nl.example.backend.features.authorization.service.AuthorizationSeeder
import nl.example.backend.features.authorization.service.AuthorizationService
import nl.example.backend.features.authorization.service.RoleService
import nl.example.backend.features.feedback.repository.ExposedFeedbackRepository
import nl.example.backend.features.feedback.service.FeedbackService
import nl.example.backend.features.user.repository.ExposedUserRepository
import nl.example.backend.features.user.service.UserService
import nl.example.backend.plugins.configureAuthentication
import nl.example.backend.plugins.configureCallLogging
import nl.example.backend.plugins.configureRateLimiting
import nl.example.backend.plugins.configureRouting
import nl.example.backend.plugins.configureSerialization
import nl.example.backend.plugins.configureStatusPages

fun main() {
    val config = AppConfig.load()

    DatabaseFactory.init(config)

    embeddedServer(
        factory = Netty,
        port = config.port,
        host = config.host,
        module = {
            module(config)
        }
    ).start(wait = true)
}

/** Creates every object once and wires them together (constructor injection, no DI framework). */
fun Application.module(config: AppConfig = AppConfig.load()) {
    // Repositories
    val userRepository = ExposedUserRepository()
    val authorizationRepository = ExposedAuthorizationRepository()
    val feedbackRepository = ExposedFeedbackRepository()

    AuthorizationSeeder(authorizationRepository).seed()

    // Services
    val jwtService = JwtService(config)
    val authorizationService = AuthorizationService(authorizationRepository, userRepository)
    val roleService = RoleService(authorizationRepository, userRepository)
    val userService = UserService(userRepository)
    val authenticationService = AuthenticationService(
        userRepository = userRepository,
        passwordHasher = PasswordHasher(),
        jwtService = jwtService,
        roleService = roleService,
        bootstrapAdminEmail = config.bootstrapAdminEmail
    )
    val feedbackService = FeedbackService(feedbackRepository)

    // Plugins and routes
    configureCallLogging()
    configureSerialization()
    configureStatusPages()
    configureRateLimiting()
    configureAuthentication(jwtService, userRepository)
    configureRouting(authenticationService, userService, roleService, authorizationService, feedbackService)
}
