package nl.brul.backend.features.help.mapper

import nl.brul.backend.features.help.dto.HelpParticipationResponse
import nl.brul.backend.features.help.dto.HelpParticipationSummaryResponse
import nl.brul.backend.features.help.model.HelpParticipation
import nl.brul.backend.features.help.model.HelpParticipationSummary

/**
 * Extension functions mapping domain models to network DTO responses.
 */
fun HelpParticipation.toResponse(): HelpParticipationResponse =
    HelpParticipationResponse(
        id = id.toString(),
        helpRequestId = helpRequestId.toString(),
        userId = userId.toString(),
        status = status.name,
        checkedInAt = checkedInAt?.toString()
    )

fun HelpParticipationSummary.toResponse(): HelpParticipationSummaryResponse =
    HelpParticipationSummaryResponse(
        helpRequestId = helpRequestId.toString(),
        volunteersNeeded = volunteersNeeded,
        participantCount = participantCount,
        availablePlaces = availablePlaces,
        isFull = isFull
    )
