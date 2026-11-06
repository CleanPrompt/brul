package nl.brul.backend.features.user.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val displayName: String,
    val active: Boolean,
    /** ISO 8601 in UTC, e.g. `2026-11-04T14:25:07Z`. */
    val createdAt: String
)
