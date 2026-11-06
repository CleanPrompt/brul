package nl.example.backend.features.authorization.service

import nl.example.backend.features.authorization.dto.RoleResponse
import nl.example.backend.features.authorization.mapper.toResponse
import nl.example.backend.features.authorization.model.DefaultRole
import nl.example.backend.features.authorization.model.Role
import nl.example.backend.features.authorization.repository.AuthorizationRepository
import nl.example.backend.features.user.repository.UserRepository
import nl.example.backend.shared.error.AppException
import nl.example.backend.shared.error.requireValid
import java.util.UUID

/** Assigns and revokes roles. Roles and permissions themselves are created by [AuthorizationSeeder]. */
class RoleService(
    private val authorizationRepository: AuthorizationRepository,
    private val userRepository: UserRepository
) {
    fun findAllRoles(): List<RoleResponse> =
        authorizationRepository.findAllRoles().map { it.toResponse() }

    fun findRolesForUser(userId: UUID): List<RoleResponse> {
        requireUserExists(userId)
        return authorizationRepository.findRolesForUser(userId).map { it.toResponse() }
    }

    fun assignRole(userId: UUID, roleId: UUID) {
        requireUserExists(userId)
        val role = findRole(roleId)
        authorizationRepository.assignRoleToUser(userId, role.id)
    }

    /**
     * Admins cannot remove their own ADMIN role. The caller of this method always has `role.assign`, so it keeps
     * at least one admin in the system: only another admin can take the role away.
     */
    fun removeRole(userId: UUID, roleId: UUID, currentUserId: UUID) {
        requireUserExists(userId)
        val role = findRole(roleId)
        requireValid(userId != currentUserId || role.name != DefaultRole.ADMIN.name) {
            "You cannot remove your own ADMIN role"
        }

        authorizationRepository.removeRoleFromUser(userId, role.id)
    }

    /** Used at registration; the default roles always exist because the seeder runs at startup. */
    fun assignDefaultRole(userId: UUID, defaultRole: DefaultRole) {
        val role = authorizationRepository.findRoleByName(defaultRole.name)
            ?: error("Default role ${defaultRole.name} has not been seeded")
        authorizationRepository.assignRoleToUser(userId, role.id)
    }

    private fun requireUserExists(userId: UUID) {
        userRepository.findById(userId) ?: throw AppException.NotFound("User not found", "USER_NOT_FOUND")
    }

    private fun findRole(roleId: UUID): Role =
        authorizationRepository.findRoleById(roleId)
            ?: throw AppException.NotFound("Role not found", "ROLE_NOT_FOUND")
}
