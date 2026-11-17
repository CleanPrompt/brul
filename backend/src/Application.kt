package nl.brul.backend

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import nl.brul.backend.config.AppConfig
import nl.brul.backend.config.DatabaseFactory
import nl.brul.backend.features.authorization.repository.ExposedAuthorizationRepository
import nl.brul.backend.features.authorization.service.AuthorizationSeeder
import nl.brul.backend.features.feedback.repository.ExposedFeedbackRepository
import nl.brul.backend.features.user.repository.ExposedProfileImageRepository
import nl.brul.backend.features.user.repository.ExposedUserRepository
import nl.brul.backend.plugins.configureAuthentication
import nl.brul.backend.plugins.configureCallLogging
import nl.brul.backend.plugins.configureRateLimiting
import nl.brul.backend.plugins.configureRouting
import nl.brul.backend.plugins.configureSerialization
import nl.brul.backend.plugins.configureStatusPages

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

/** The real application: the Exposed repositories, seeded roles and permissions, and the shared wiring. */
fun Application.module(config: AppConfig) {
    val authorizationRepository = ExposedAuthorizationRepository()
    AuthorizationSeeder(authorizationRepository).seed()

    val services = AppServices(
        userRepository = ExposedUserRepository(),
        authorizationRepository = authorizationRepository,
        feedbackRepository = ExposedFeedbackRepository(),
        profileImageRepository = ExposedProfileImageRepository(),
        config = config
    )
    configureApp(services)
}

/** Installs the plugins and routes. `module()` and the tests both call it, so tests run the real wiring. */
fun Application.configureApp(services: AppServices) {
    configureCallLogging()
    configureSerialization()
    configureStatusPages()
    configureRateLimiting()
    configureAuthentication(services.jwtService, services.userRepository)
    configureRouting(services)
}
