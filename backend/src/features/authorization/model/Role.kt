package nl.example.backend.features.authorization.model

import java.util.UUID

data class Role(
    val id: UUID,
    val name: String
)