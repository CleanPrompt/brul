package nl.brul.backend.features.authorization.database

import org.jetbrains.exposed.sql.Table

object RolePermissionsTable : Table("role_permissions") {
    val roleId = uuid("role_id").references(RolesTable.id)
    val permissionId = uuid("permission_id").references(PermissionsTable.id)

    override val primaryKey = PrimaryKey(roleId, permissionId)
}