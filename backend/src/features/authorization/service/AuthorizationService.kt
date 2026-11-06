package nl.example.backend.features.authorization.service

import nl.example.backend.features.authorization.model.Permission
import nl.example.backend.features.authorization.repository.AuthorizationRepository
import nl.example.backend.features.user.repository.UserRepository
import nl.example.backend.shared.error.AppException
import java.util.UUID

/**
 * All access rules in one place. Permissions are read from the database on every call, so a revoked
 * role takes effect immediately, even for a token that was issued earlier.
 */
class AuthorizationService(
    private val authorizationRepository: AuthorizationRepository,
    private val userRepository: UserRepository
) {
    fun hasPermission(userId: UUID, permission: Permission): Boolean {
        val user = userRepository.findById(userId) ?: return false

        return user.active && permission in authorizationRepository.findPermissionsForUser(userId)
    }

    /** Throws 403 unless the user has [permission]. */
    fun requirePermission(userId: UUID, permission: Permission) {
        if (!hasPermission(userId, permission)) {
            throw AppException.Forbidden("Missing permission: ${permission.key}")
        }
    }

    /**
     * Owners may always access their own resource; others need [permission]. [ownerId] is the user id of the owner:
     * `feedback.userId` for a feedback item, or the account's own id for a user account (users own their account).
     */
    fun requireOwnerOrPermission(currentUserId: UUID, ownerId: UUID, permission: Permission) {
        if (currentUserId != ownerId) {
            requirePermission(currentUserId, permission)
        }
    }
}
