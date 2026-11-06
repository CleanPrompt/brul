package nl.example.backend.features.authorization.mapper

import nl.example.backend.features.authorization.database.RolesTable
import nl.example.backend.features.authorization.dto.RoleResponse
import nl.example.backend.features.authorization.model.Role
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toRole(): Role =
    Role(
        id = this[RolesTable.id],
        name = this[RolesTable.name]
    )

fun Role.toResponse(): RoleResponse =
    RoleResponse(
        id = id.toString(),
        name = name
    )
