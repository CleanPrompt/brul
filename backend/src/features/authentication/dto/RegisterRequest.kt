package nl.example.backend.features.authentication.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val displayName: String,
    val password: String
)