package nl.example.backend.features.authorization.database

import org.jetbrains.exposed.sql.Table

object RolesTable : Table("roles") {
    val id = uuid("id")
    val name = varchar("name", 100).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}