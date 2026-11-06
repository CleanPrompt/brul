package nl.brul.backend.features.user.mapper

import nl.brul.backend.features.user.dto.UserResponse
import nl.brul.backend.features.user.model.User

/** Public representation of a user; deliberately leaves out the password hash. */
fun User.toResponse(): UserResponse =
    UserResponse(
        id = id.toString(),
        email = email,
        displayName = displayName,
        active = active,
        createdAt = createdAt.toString()
    )
