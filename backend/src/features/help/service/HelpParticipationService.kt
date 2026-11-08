package nl.brul.backend.features.help.service

import nl.brul.backend.features.help.dto.CheckInCodeResponse
import nl.brul.backend.features.help.dto.CheckInResponse
import nl.brul.backend.features.help.model.CheckInRejection
import nl.brul.backend.features.help.model.CheckInResult
import nl.brul.backend.features.help.model.HelpCalendarItem
import nl.brul.backend.features.help.model.HelpParticipation
import nl.brul.backend.features.help.model.HelpParticipationSummary
import nl.brul.backend.features.help.model.Location
import nl.brul.backend.features.help.model.ParticipationStatus
import nl.brul.backend.features.help.repository.HelpParticipationRepository
import nl.brul.backend.shared.error.AppException
import java.time.Instant
import java.util.UUID

/**
 * Domain service orchestrating volunteer registrations, capacity checks, and check-ins.
 */
class HelpParticipationService(
    private val participationRepository: HelpParticipationRepository,
    private val helpRequestProvider: HelpRequestProvider,
    private val currentTime: () -> Instant = Instant::now
) {

    /**
     * Signs up a volunteer for a help request.
     */
    fun join(helpRequestId: UUID, userId: UUID): HelpParticipation {
        val request = findHelpRequestOrThrow(helpRequestId)

        if (request.ownerId == userId) {
            throw AppException.Forbidden("You cannot join your own help request.")
        }

        if (!request.isOpen) {
            throw AppException.Conflict("This help request is closed.", "HELP_REQUEST_CLOSED")
        }

        val existing = participationRepository.findByHelpRequestAndUser(helpRequestId, userId)
        if (existing != null) {
            throw AppException.Conflict(
                "You already participate in this help request.",
                "ALREADY_PARTICIPATING"
            )
        }

        val participantCount = participationRepository.countByHelpRequest(helpRequestId)
        if (participantCount >= request.volunteersNeeded) {
            throw AppException.Conflict("This help request is full.", "HELP_REQUEST_FULL")
        }

        val participation = HelpParticipation(
            id = UUID.randomUUID(),
            helpRequestId = helpRequestId,
            userId = userId,
            status = ParticipationStatus.SIGNED_UP
        )

        return participationRepository.save(participation)
    }

    /**
     * Removes a volunteer's registration.
     */
    fun leave(helpRequestId: UUID, userId: UUID): Boolean {
        findHelpRequestOrThrow(helpRequestId)

        val removed = participationRepository.delete(helpRequestId, userId)
        if (!removed) {
            throw AppException.NotFound("Participation not found.", "PARTICIPATION_NOT_FOUND")
        }
        return true
    }

    /**
     * Retrieves all volunteers for a help request.
     */
    fun getParticipants(helpRequestId: UUID): List<HelpParticipation> {
        findHelpRequestOrThrow(helpRequestId)
        return participationRepository.findByHelpRequest(helpRequestId)
    }

    /**
     * Returns a capacity and availability summary.
     */
    fun getSummary(helpRequestId: UUID): HelpParticipationSummary {
        val request = findHelpRequestOrThrow(helpRequestId)
        val count = participationRepository.countByHelpRequest(helpRequestId)
        val available = (request.volunteersNeeded - count).coerceAtLeast(0)
        val isFull = available == 0 || !request.isOpen

        return HelpParticipationSummary(
            helpRequestId = helpRequestId,
            volunteersNeeded = request.volunteersNeeded,
            participantCount = count,
            availablePlaces = available,
            isFull = isFull
        )
    }

    /**
     * Generates a 6-digit check-in code. Only the owner of the help request can perform this.
     */
    fun generateCheckInCode(helpRequestId: UUID, userId: UUID): CheckInCodeResponse {
        val request = findHelpRequestOrThrow(helpRequestId)

        if (request.ownerId != userId) {
            throw AppException.Forbidden("Only the owner can generate a check-in code.", "NOT_HELP_REQUEST_OWNER")
        }

        if (!request.isOpen) {
            throw AppException.Conflict(
                "Cannot generate check-in code for a closed help request.",
                "HELP_REQUEST_CLOSED"
            )
        }

        val code = (100000..999999).random().toString()
        val expiresAt = request.endTime
        helpRequestProvider.setCheckInCode(helpRequestId, code, expiresAt)

        return CheckInCodeResponse(
            helpRequestId = helpRequestId.toString(),
            code = code,
            expiresAt = expiresAt.toString()
        )
    }

    /**
     * Validates a volunteer's check-in code, timing, and GPS location.
     */
    fun checkIn(
        helpRequestId: UUID,
        userId: UUID,
        code: String,
        volunteerLocation: Location?
    ): CheckInResponse {
        val request = findHelpRequestOrThrow(helpRequestId)

        val participation = participationRepository.findByHelpRequestAndUser(helpRequestId, userId)
            ?: throw AppException.NotFound(
                "You are not registered as a volunteer for this help request.",
                "NOT_REGISTERED"
            )

        val stored = helpRequestProvider.getCheckInCode(helpRequestId)
            ?: throw AppException.BadRequest(
                "No active check-in code has been generated for this help request.",
                "NO_ACTIVE_CHECK_IN_CODE"
            )

        val now = currentTime()
        val result = CheckInValidator.validate(
            expectedCode = stored.code,
            providedCode = code,
            startTime = request.startTime,
            endTime = request.endTime,
            now = now,
            requestLocation = request.location,
            volunteerLocation = volunteerLocation,
            currentStatus = participation.status
        )

        when (result) {
            is CheckInResult.Success -> {
                participationRepository.updateStatus(
                    helpRequestId = helpRequestId,
                    userId = userId,
                    status = ParticipationStatus.ATTENDED,
                    checkedInAt = now
                )

                return CheckInResponse(
                    success = true,
                    message = "Check-in successful.",
                    checkedInAt = now.toString()
                )
            }

            is CheckInResult.Rejected -> {
                val (message, errorCode) = when (result.reason) {
                    CheckInRejection.INVALID_CODE ->
                        "Invalid check-in code." to "INVALID_CHECK_IN_CODE"
                    CheckInRejection.TOO_EARLY ->
                        "Check-in is only available starting 15 minutes before the start time." to "CHECK_IN_TOO_EARLY"
                    CheckInRejection.TOO_LATE ->
                        "Check-in window has expired." to "CHECK_IN_TOO_LATE"
                    CheckInRejection.NOT_SIGNED_UP ->
                        "You are not registered as a volunteer for this help request." to "NOT_SIGNED_UP"
                    CheckInRejection.ALREADY_ATTENDED ->
                        "Attendance has already been registered." to "ALREADY_ATTENDED"
                    CheckInRejection.LOCATION_TOO_FAR ->
                        "You are too far from the help request location (max 150m)." to "LOCATION_TOO_FAR"
                }
                throw AppException.BadRequest(message, errorCode)
            }
        }
    }

    /**
     * Generates an RFC 5545 iCalendar feed string.
     */
    fun getCalendarFeed(helpRequestId: UUID): String {
        val request = findHelpRequestOrThrow(helpRequestId)
        val item = HelpCalendarItem(
            id = request.id,
            title = request.title,
            description = request.description,
            category = request.category,
            location = request.location
        )

        return CalendarFeedService.generateIcs(
            item = item,
            startTime = request.startTime,
            endTime = request.endTime,
            now = currentTime()
        )
    }

    private fun findHelpRequestOrThrow(id: UUID): HelpRequestInfo =
        helpRequestProvider.findHelpRequest(id)
            ?: throw AppException.NotFound("Help request not found.", "HELP_REQUEST_NOT_FOUND")
}
