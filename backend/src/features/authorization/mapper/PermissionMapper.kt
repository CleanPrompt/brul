package nl.example.backend.features.authorization.mapper

import nl.example.backend.features.authorization.database.PermissionsTable
import nl.example.backend.features.authorization.model.Permission
import org.jetbrains.exposed.sql.ResultRow

/** Returns `null` for a key that is no longer defined in [Permission]. */
fun ResultRow.toPermission(): Permission? =
    Permission.fromKey(this[PermissionsTable.name])
