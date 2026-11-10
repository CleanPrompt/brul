package nl.brul.backend.features.vote.mapper

import nl.brul.backend.features.vote.dto.VoteResponse
import nl.brul.backend.features.vote.dto.VoteSummaryResponse
import nl.brul.backend.features.vote.model.Vote
import nl.brul.backend.features.vote.model.VoteSummary

fun Vote.toResponse(): VoteResponse =
    VoteResponse(
        id = id.toString(),
        feedbackId = feedbackId.toString(),
        userId = userId.toString(),
        type = type
    )

fun VoteSummary.toResponse(): VoteSummaryResponse =
    VoteSummaryResponse(
        feedbackId = feedbackId.toString(),
        likes = likes,
        dislikes = dislikes,
        score = score,
        totalVotes = totalVotes
    )
