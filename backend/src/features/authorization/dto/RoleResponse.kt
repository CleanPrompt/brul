package nl.example.backend.features.authorization.dto

import kotlinx.serialization.Serializable

@Serializable
data class RoleResponse(
    val id: String,
    val name: String
)
