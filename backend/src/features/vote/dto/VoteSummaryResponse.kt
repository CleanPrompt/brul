package nl.brul.backend.features.vote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VoteSummaryResponse(
    val feedbackId: String,
    val likes: Int,
    val dislikes: Int,
    val score: Int,
    val totalVotes: Int
)
