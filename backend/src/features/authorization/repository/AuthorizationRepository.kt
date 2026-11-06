package nl.brul.backend.features.authorization.repository

import nl.brul.backend.features.authorization.model.Permission
import nl.brul.backend.features.authorization.model.Role
import java.util.UUID

interface AuthorizationRepository {
    fun findRoleById(id: UUID): Role?
    fun findRoleByName(name: String): Role?
    fun findAllRoles(): List<Role>
    fun createRole(role: Role): Role

    fun createPermissionIfMissing(permission: Permission)
    fun grantPermissionToRole(roleId: UUID, permission: Permission)

    fun assignRoleToUser(userId: UUID, roleId: UUID)
    /** Returns `false` when the user did not have the role. */
    fun removeRoleFromUser(userId: UUID, roleId: UUID): Boolean

    fun findRolesForUser(userId: UUID): List<Role>
    fun findPermissionsForUser(userId: UUID): Set<Permission>
}
