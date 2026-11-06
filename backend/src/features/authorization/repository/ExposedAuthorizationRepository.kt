package nl.example.backend.features.authorization.repository

import nl.example.backend.features.authorization.database.PermissionsTable
import nl.example.backend.features.authorization.database.RolePermissionsTable
import nl.example.backend.features.authorization.database.RolesTable
import nl.example.backend.features.authorization.database.UserRolesTable
import nl.example.backend.features.authorization.mapper.toPermission
import nl.example.backend.features.authorization.mapper.toRole
import nl.example.backend.features.authorization.model.Permission
import nl.example.backend.features.authorization.model.Role
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ExposedAuthorizationRepository : AuthorizationRepository {

    override fun findRoleById(id: UUID): Role? = transaction {
        RolesTable.selectAll()
            .where { RolesTable.id eq id }
            .singleOrNull()
            ?.toRole()
    }

    override fun findRoleByName(name: String): Role? = transaction {
        RolesTable.selectAll()
            .where { RolesTable.name eq name }
            .singleOrNull()
            ?.toRole()
    }

    override fun findAllRoles(): List<Role> = transaction {
        RolesTable.selectAll().map { it.toRole() }
    }

    override fun createRole(role: Role): Role = transaction {
        RolesTable.insert {
            it[id] = role.id
            it[name] = role.name
        }
        role
    }

    override fun createPermissionIfMissing(permission: Permission) {
        transaction {
            val exists = PermissionsTable.selectAll()
                .where { PermissionsTable.name eq permission.key }
                .any()

            if (!exists) {
                PermissionsTable.insert {
                    it[id] = UUID.randomUUID()
                    it[name] = permission.key
                }
            }
        }
    }

    override fun grantPermissionToRole(roleId: UUID, permission: Permission) {
        transaction {
            val permissionId = PermissionsTable.selectAll()
                .where { PermissionsTable.name eq permission.key }
                .singleOrNull()
                ?.get(PermissionsTable.id)
                ?: error("Permission ${permission.key} has not been created")

            val exists = RolePermissionsTable.selectAll()
                .where { (RolePermissionsTable.roleId eq roleId) and (RolePermissionsTable.permissionId eq permissionId) }
                .any()

            if (!exists) {
                RolePermissionsTable.insert {
                    it[RolePermissionsTable.roleId] = roleId
                    it[RolePermissionsTable.permissionId] = permissionId
                }
            }
        }
    }

    override fun assignRoleToUser(userId: UUID, roleId: UUID) {
        transaction {
            val exists = UserRolesTable.selectAll()
                .where { (UserRolesTable.userId eq userId) and (UserRolesTable.roleId eq roleId) }
                .any()

            if (!exists) {
                UserRolesTable.insert {
                    it[UserRolesTable.userId] = userId
                    it[UserRolesTable.roleId] = roleId
                }
            }
        }
    }

    override fun removeRoleFromUser(userId: UUID, roleId: UUID): Boolean = transaction {
        UserRolesTable.deleteWhere {
            (UserRolesTable.userId eq userId) and (UserRolesTable.roleId eq roleId)
        } > 0
    }

    override fun findRolesForUser(userId: UUID): List<Role> = transaction {
        (UserRolesTable innerJoin RolesTable)
            .selectAll()
            .where { UserRolesTable.userId eq userId }
            .map { it.toRole() }
    }

    override fun findPermissionsForUser(userId: UUID): Set<Permission> = transaction {
        // user_roles and role_permissions both reference roles, not each other, so join on role_id explicitly.
        UserRolesTable
            .join(RolePermissionsTable, JoinType.INNER, UserRolesTable.roleId, RolePermissionsTable.roleId)
            .join(PermissionsTable, JoinType.INNER, RolePermissionsTable.permissionId, PermissionsTable.id)
            .selectAll()
            .where { UserRolesTable.userId eq userId }
            .mapNotNull { it.toPermission() }
            .toSet()
    }
}
