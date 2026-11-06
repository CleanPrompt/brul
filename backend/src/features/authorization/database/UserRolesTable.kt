package nl.brul.backend.features.authorization.database

import nl.brul.backend.features.user.database.UsersTable
import org.jetbrains.exposed.sql.Table

object UserRolesTable : Table("user_roles") {
    val userId = uuid("user_id").references(UsersTable.id)
    val roleId = uuid("role_id").references(RolesTable.id)

    override val primaryKey = PrimaryKey(userId, roleId)
}