package nl.brul.backend.features.vote.dto

import kotlinx.serialization.Serializable
import nl.brul.backend.features.vote.model.VoteType

@Serializable
data class VoteRequest(
    val type: VoteType
)
