package nl.brul.backend.features.authentication.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
