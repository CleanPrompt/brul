package nl.brul.backend.features.vote.model

import java.util.UUID

data class VoteSummary(
    val feedbackId: UUID,
    val likes: Int,
    val dislikes: Int
) {
    val score: Int
        get() = likes - dislikes

    val totalVotes: Int
        get() = likes + dislikes
}
