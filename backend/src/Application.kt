package nl.brul.backend

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import nl.brul.backend.config.AppConfig
import nl.brul.backend.config.DatabaseFactory
import nl.brul.backend.features.authentication.service.AuthenticationService
import nl.brul.backend.features.authentication.service.JwtService
import nl.brul.backend.features.authentication.service.PasswordHasher
import nl.brul.backend.features.authorization.repository.ExposedAuthorizationRepository
import nl.brul.backend.features.authorization.service.AuthorizationSeeder
import nl.brul.backend.features.authorization.service.AuthorizationService
import nl.brul.backend.features.authorization.service.RoleService
import nl.brul.backend.features.feedback.repository.ExposedFeedbackRepository
import nl.brul.backend.features.feedback.service.FeedbackService
import nl.brul.backend.features.report.repository.ExposedReportPhotoRepository
import nl.brul.backend.features.report.repository.ExposedReportRepository
import nl.brul.backend.features.report.repository.FileSystemPhotoStorage
import nl.brul.backend.features.report.service.ReportPhotoService
import nl.brul.backend.features.report.service.ReportService
import nl.brul.backend.features.user.repository.ExposedUserRepository
import nl.brul.backend.features.user.service.UserService
import nl.brul.backend.features.vote.repository.ExposedVoteRepository
import nl.brul.backend.features.vote.service.VoteService
import nl.brul.backend.plugins.configureAuthentication
import nl.brul.backend.plugins.configureCallLogging
import nl.brul.backend.plugins.configureRateLimiting
import nl.brul.backend.plugins.configureRouting
import nl.brul.backend.plugins.configureSerialization
import nl.brul.backend.plugins.configureStatusPages
import java.nio.file.Path

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
    val reportRepository = ExposedReportRepository()
    val reportPhotoRepository = ExposedReportPhotoRepository()
    // Photo files live next to the database file; the folder is ignored by Git.
    val photoStorage = FileSystemPhotoStorage(Path.of("data", "uploads"))
    val voteRepository = ExposedVoteRepository()

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
    val reportPhotoService = ReportPhotoService(reportRepository, reportPhotoRepository, photoStorage)
    val reportService = ReportService(reportRepository, reportPhotoService)
    val voteService = VoteService(voteRepository, feedbackService)

    // Plugins and routes
    configureCallLogging()
    configureSerialization()
    configureStatusPages()
    configureRateLimiting()
    configureAuthentication(jwtService, userRepository)
    configureRouting(
        authenticationService,
        userService,
        roleService,
        authorizationService,
        feedbackService,
        reportService,
        reportPhotoService
        voteService
    )
}
