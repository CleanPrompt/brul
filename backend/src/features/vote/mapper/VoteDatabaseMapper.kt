package nl.brul.backend.features.vote.mapper

import nl.brul.backend.features.vote.database.VotesTable
import nl.brul.backend.features.vote.model.Vote
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toVote(): Vote =
    Vote(
        id = this[VotesTable.id],
        feedbackId = this[VotesTable.feedbackId],
        userId = this[VotesTable.userId],
        type = this[VotesTable.type]
    )
