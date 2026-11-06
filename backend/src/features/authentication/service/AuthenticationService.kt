package nl.brul.backend.features.authentication.service

import nl.brul.backend.features.authentication.dto.AuthenticationResponse
import nl.brul.backend.features.authentication.dto.ChangePasswordRequest
import nl.brul.backend.features.authentication.dto.LoginRequest
import nl.brul.backend.features.authentication.dto.RegisterRequest
import nl.brul.backend.features.authentication.mapper.toAuthenticationResponse
import nl.brul.backend.features.authorization.model.DefaultRole
import nl.brul.backend.features.authorization.service.RoleService
import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.repository.UserRepository
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.error.requireValid
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class AuthenticationService(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtService: JwtService,
    private val roleService: RoleService,
    /** The account registering with this email also becomes ADMIN; see `BOOTSTRAP_ADMIN_EMAIL`. */
    private val bootstrapAdminEmail: String?
) {
    fun register(request: RegisterRequest): AuthenticationResponse {
        val email = request.email.trim().lowercase()
        val displayName = request.displayName.trim()

        requireValid(email.length <= 254 && EMAIL_PATTERN.matches(email)) {
            "Invalid email address"
        }
        requireValid(displayName.isNotEmpty() && displayName.length <= 120) {
            "Display name must contain between 1 and 120 characters"
        }
        requireValidPassword(request.password)

        if (userRepository.findByEmail(email) != null) {
            throw AppException.Conflict("Email address is already registered", "EMAIL_ALREADY_REGISTERED")
        }

        val user = User(
            id = UUID.randomUUID(),
            email = email,
            displayName = displayName,
            passwordHash = passwordHasher.hash(request.password),
            active = true,
            // Whole seconds: the API shows a clean timestamp and the database stores it without rounding.
            createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )

        val savedUser = userRepository.save(user)

        roleService.assignDefaultRole(savedUser.id, DefaultRole.USER)
        if (savedUser.email == bootstrapAdminEmail) {
            roleService.assignDefaultRole(savedUser.id, DefaultRole.ADMIN)
        }

        return createResponse(savedUser)
    }

    /**
     * Every failure gives the same error, so a caller cannot find out which emails are registered.
     */
    fun login(request: LoginRequest): AuthenticationResponse {
        val email = request.email.trim().lowercase()
        val user = userRepository.findByEmail(email)

        if (user == null || !user.active || !passwordHasher.verify(request.password, user.passwordHash)) {
            throw AppException.Unauthorized("Invalid email or password", "INVALID_CREDENTIALS")
        }

        return createResponse(user)
    }

    /** Logs the user out on every device: all tokens issued so far stop working. */
    fun logout(userId: UUID) {
        userRepository.incrementTokenVersion(userId)
    }

    /**
     * The current password is required, so a stolen token alone cannot take over the account.
     * Every earlier token stops working (like logout); the response holds a new token, so this device
     * stays logged in.
     */
    fun changePassword(userId: UUID, request: ChangePasswordRequest): AuthenticationResponse {
        requireValidPassword(request.newPassword)
        requireValid(request.newPassword != request.currentPassword) {
            "New password must differ from the current password"
        }

        val user = findUser(userId)
        if (!passwordHasher.verify(request.currentPassword, user.passwordHash)) {
            throw AppException.BadRequest("Current password is incorrect", "INVALID_CURRENT_PASSWORD")
        }

        userRepository.update(user.copy(passwordHash = passwordHasher.hash(request.newPassword)))
        userRepository.incrementTokenVersion(userId)

        // Read the user again to get the new token version.
        return createResponse(findUser(userId))
    }

    /** BCrypt only uses the first 72 bytes of a password, hence the upper limit. */
    private fun requireValidPassword(password: String) {
        requireValid(password.length in 12..72) {
            "Password must contain between 12 and 72 characters"
        }
    }

    private fun findUser(id: UUID): User =
        userRepository.findById(id) ?: throw AppException.NotFound("User not found", "USER_NOT_FOUND")

    private fun createResponse(user: User): AuthenticationResponse =
        user.toAuthenticationResponse(
            accessToken = jwtService.generateToken(user.id, user.tokenVersion),
            expiresInSeconds = jwtService.expiresInSeconds
        )

    companion object {
        private val EMAIL_PATTERN =
            Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
