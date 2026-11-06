package nl.brul.backend.features.authorization.mapper

import nl.brul.backend.features.authorization.database.RolesTable
import nl.brul.backend.features.authorization.dto.RoleResponse
import nl.brul.backend.features.authorization.model.Role
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
