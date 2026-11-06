package nl.example.backend.features.user.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    val displayName: String
)
