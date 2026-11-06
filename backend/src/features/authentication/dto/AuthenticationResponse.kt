package nl.brul.backend.features.authentication.dto

import kotlinx.serialization.Serializable
import nl.brul.backend.features.user.dto.UserResponse

@Serializable
data class AuthenticationResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponse
)