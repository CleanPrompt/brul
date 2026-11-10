package nl.brul.backend.features.vote.model

import java.util.UUID

data class Vote(
    val id: UUID,
    val feedbackId: UUID,
    val userId: UUID,
    val type: VoteType
)
