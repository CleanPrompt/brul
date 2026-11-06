package nl.brul.backend.shared.error

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String
)
