package nl.brul.backend

import nl.brul.backend.config.AppConfig
import nl.brul.backend.features.authentication.service.AuthenticationService
import nl.brul.backend.features.authentication.service.JwtService
import nl.brul.backend.features.authentication.service.PasswordHasher
import nl.brul.backend.features.authorization.repository.AuthorizationRepository
import nl.brul.backend.features.authorization.service.AuthorizationService
import nl.brul.backend.features.authorization.service.RoleService
import nl.brul.backend.features.feedback.repository.FeedbackRepository
import nl.brul.backend.features.feedback.service.FeedbackService
import nl.brul.backend.features.user.repository.ProfileImageRepository
import nl.brul.backend.features.user.repository.UserRepository
import nl.brul.backend.features.user.service.ProfileImageService
import nl.brul.backend.features.user.service.UserService

/**
 * Builds every service once, from the repositories and the configuration (constructor injection, no DI framework).
 * `module()` passes the Exposed repositories and the tests pass fakes, so both use exactly the same wiring.
 */
class AppServices(
    val userRepository: UserRepository,
    val authorizationRepository: AuthorizationRepository,
    val feedbackRepository: FeedbackRepository,
    val profileImageRepository: ProfileImageRepository,
    config: AppConfig
) {
    val jwtService = JwtService(config)
    val authorizationService = AuthorizationService(authorizationRepository, userRepository)
    val userService = UserService(userRepository)
    val roleService = RoleService(authorizationRepository, userService)
    val authenticationService = AuthenticationService(
        userRepository = userRepository,
        passwordHasher = PasswordHasher(),
        jwtService = jwtService,
        roleService = roleService,
        bootstrapAdminEmail = config.bootstrapAdminEmail
    )
    val feedbackService = FeedbackService(feedbackRepository)
    val profileImageService = ProfileImageService(profileImageRepository)
}
