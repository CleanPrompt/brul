package nl.example.backend.features.authorization.database

import org.jetbrains.exposed.sql.Table

object PermissionsTable : Table("permissions") {
    val id = uuid("id")
    val name = varchar("name", 150).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}