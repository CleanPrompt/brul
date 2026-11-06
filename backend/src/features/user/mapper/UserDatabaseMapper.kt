package nl.brul.backend.features.user.mapper

import nl.brul.backend.features.user.database.UsersTable
import nl.brul.backend.features.user.model.User
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toUser(): User =
    User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        displayName = this[UsersTable.displayName],
        passwordHash = this[UsersTable.passwordHash],
        active = this[UsersTable.active],
        createdAt = this[UsersTable.createdAt],
        tokenVersion = this[UsersTable.tokenVersion]
    )
