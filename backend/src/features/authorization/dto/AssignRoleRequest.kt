package nl.example.backend.features.authorization.dto

import kotlinx.serialization.Serializable

@Serializable
data class AssignRoleRequest(
    val roleId: String
)
