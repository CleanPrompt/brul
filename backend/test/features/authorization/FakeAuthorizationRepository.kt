package nl.brul.backend.features.authorization

import nl.brul.backend.features.authorization.model.Permission
import nl.brul.backend.features.authorization.model.Role
import nl.brul.backend.features.authorization.repository.AuthorizationRepository
import java.util.UUID

/** In-memory [AuthorizationRepository] so service and route tests do not need a database. */
class FakeAuthorizationRepository : AuthorizationRepository {
    private val roles = mutableMapOf<UUID, Role>()
    private val permissions = mutableSetOf<Permission>()
    private val rolePermissions = mutableMapOf<UUID, MutableSet<Permission>>()
    private val userRoles = mutableMapOf<UUID, MutableSet<UUID>>()

    override fun findRoleById(id: UUID): Role? = roles[id]

    override fun findRoleByName(name: String): Role? = roles.values.find { it.name == name }

    override fun findAllRoles(): List<Role> = roles.values.toList()

    override fun createRole(role: Role): Role = role.also { roles[it.id] = it }

    override fun createPermissionIfMissing(permission: Permission) {
        permissions += permission
    }

    override fun grantPermissionToRole(roleId: UUID, permission: Permission) {
        check(permission in permissions) { "Permission ${permission.key} has not been created" }
        rolePermissions.getOrPut(roleId) { mutableSetOf() } += permission
    }

    override fun assignRoleToUser(userId: UUID, roleId: UUID) {
        userRoles.getOrPut(userId) { mutableSetOf() } += roleId
    }

    override fun removeRoleFromUser(userId: UUID, roleId: UUID): Boolean =
        userRoles[userId]?.remove(roleId) ?: false

    override fun findRolesForUser(userId: UUID): List<Role> =
        userRoles[userId].orEmpty().mapNotNull { roles[it] }

    override fun findPermissionsForUser(userId: UUID): Set<Permission> =
        userRoles[userId].orEmpty().flatMap { rolePermissions[it].orEmpty() }.toSet()
}
