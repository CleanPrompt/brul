package nl.brul.backend.features.vote.dto

import kotlinx.serialization.Serializable
import nl.brul.backend.features.vote.model.VoteType

@Serializable
data class VoteResponse(
    val id: String,
    val feedbackId: String,
    val userId: String,
    val type: VoteType
)
