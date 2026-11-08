package nl.brul.backend.features.help.service

import nl.brul.backend.features.help.model.CheckInRejection
import nl.brul.backend.features.help.model.CheckInResult
import nl.brul.backend.features.help.model.Location
import nl.brul.backend.features.help.model.ParticipationStatus
import java.time.Duration
import java.time.Instant

/**
 * Pure domain service for validating volunteer check-in attempts.
 * Verifies OTP code matching, status preconditions, time window, and geofencing distance.
 */
object CheckInValidator {

    const val MAX_DISTANCE_METERS = 150.0
    val CHECK_IN_WINDOW_BEFORE_START: Duration = Duration.ofMinutes(15)

    fun validate(
        expectedCode: String,
        providedCode: String,
        startTime: Instant,
        endTime: Instant,
        now: Instant,
        requestLocation: Location,
        volunteerLocation: Location?,
        currentStatus: ParticipationStatus
    ): CheckInResult {
        if (currentStatus == ParticipationStatus.ATTENDED) {
            return CheckInResult.Rejected(CheckInRejection.ALREADY_ATTENDED)
        }

        if (currentStatus != ParticipationStatus.SIGNED_UP) {
            return CheckInResult.Rejected(CheckInRejection.NOT_SIGNED_UP)
        }

        if (expectedCode.trim() != providedCode.trim()) {
            return CheckInResult.Rejected(CheckInRejection.INVALID_CODE)
        }

        val earliestAllowed = startTime.minus(CHECK_IN_WINDOW_BEFORE_START)
        if (now.isBefore(earliestAllowed)) {
            return CheckInResult.Rejected(CheckInRejection.TOO_EARLY)
        }

        if (now.isAfter(endTime)) {
            return CheckInResult.Rejected(CheckInRejection.TOO_LATE)
        }

        if (volunteerLocation != null) {
            val distanceKm = volunteerLocation.distanceTo(requestLocation)
            if (distanceKm * 1000.0 > MAX_DISTANCE_METERS) {
                return CheckInResult.Rejected(CheckInRejection.LOCATION_TOO_FAR)
            }
        }

        return CheckInResult.Success
    }
}
