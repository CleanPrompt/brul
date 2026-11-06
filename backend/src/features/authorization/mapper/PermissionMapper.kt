package nl.brul.backend.features.authorization.mapper

import nl.brul.backend.features.authorization.database.PermissionsTable
import nl.brul.backend.features.authorization.model.Permission
import org.jetbrains.exposed.sql.ResultRow

/** Returns `null` for a key that is no longer defined in [Permission]. */
fun ResultRow.toPermission(): Permission? =
    Permission.fromKey(this[PermissionsTable.name])
