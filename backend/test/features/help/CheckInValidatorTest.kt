package nl.brul.backend.features.help

import nl.brul.backend.features.help.model.CheckInRejection
import nl.brul.backend.features.help.model.CheckInResult
import nl.brul.backend.features.help.model.Location
import nl.brul.backend.features.help.model.ParticipationStatus
import nl.brul.backend.features.help.service.CheckInValidator
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckInValidatorTest {

    private val startTime = Instant.parse("2026-11-20T10:00:00Z")
    private val endTime = Instant.parse("2026-11-20T12:00:00Z")
    private val location = Location(51.582, 4.771)
    private val code = "123456"

    @Test
    fun `valid check-in within window and location succeeds`() {
        val now = startTime.plus(Duration.ofMinutes(10)) // 10:10
        val volunteerLoc = Location(51.5821, 4.7711) // ~13 meters away

        val result = CheckInValidator.validate(
            expectedCode = code,
            providedCode = code,
            startTime = startTime,
            endTime = endTime,
            now = now,
            requestLocation = location,
            volunteerLocation = volunteerLoc,
            currentStatus = ParticipationStatus.SIGNED_UP
        )

        assertEquals(CheckInResult.Success, result)
    }

    @Test
    fun `check-in 10 minutes before start succeeds because within 15 minute window`() {
        val now = startTime.minus(Duration.ofMinutes(10)) // 09:50

        val result = CheckInValidator.validate(
            expectedCode = code,
            providedCode = code,
            startTime = startTime,
            endTime = endTime,
            now = now,
            requestLocation = location,
            volunteerLocation = null,
            currentStatus = ParticipationStatus.SIGNED_UP
        )

        assertEquals(CheckInResult.Success, result)
    }

    @Test
    fun `check-in 20 minutes before start is rejected as too early`() {
        val now = startTime.minus(Duration.ofMinutes(20)) // 09:40

        val result = CheckInValidator.validate(
            expectedCode = code,
            providedCode = code,
            startTime = startTime,
            endTime = endTime,
            now = now,
            requestLocation = location,
            volunteerLocation = null,
            currentStatus = ParticipationStatus.SIGNED_UP
        )

        assertTrue(result is CheckInResult.Rejected)
        assertEquals(CheckInRejection.TOO_EARLY, result.reason)
    }

    @Test
    fun `check-in after end time is rejected as too late`() {
        val now = endTime.plus(Duration.ofMinutes(5)) // 12:05

        val result = CheckInValidator.validate(
            expectedCode = code,
            providedCode = code,
            startTime = startTime,
            endTime = endTime,
            now = now,
            requestLocation = location,
            volunteerLocation = null,
            currentStatus = ParticipationStatus.SIGNED_UP
        )

        assertTrue(result is CheckInResult.Rejected)
        assertEquals(CheckInRejection.TOO_LATE, result.reason)
    }

    @Test
    fun `wrong check-in code is rejected as invalid code`() {
        val now = startTime.plus(Duration.ofMinutes(5))

        val result = CheckInValidator.validate(
            expectedCode = code,
            providedCode = "999999",
            startTime = startTime,
            endTime = endTime,
            now = now,
            requestLocation = location,
            volunteerLocation = null,
            currentStatus = ParticipationStatus.SIGNED_UP
        )

        assertTrue(result is CheckInResult.Rejected)
        assertEquals(CheckInRejection.INVALID_CODE, result.reason)
    }

    @Test
    fun `check-in when already attended is rejected`() {
        val now = startTime.plus(Duration.ofMinutes(5))

        val result = CheckInValidator.validate(
            expectedCode = code,
            providedCode = code,
            startTime = startTime,
            endTime = endTime,
            now = now,
            requestLocation = location,
            volunteerLocation = null,
            currentStatus = ParticipationStatus.ATTENDED
        )

        assertTrue(result is CheckInResult.Rejected)
        assertEquals(CheckInRejection.ALREADY_ATTENDED, result.reason)
    }

    @Test
    fun `check-in when cancelled is rejected as not signed up`() {
        val now = startTime.plus(Duration.ofMinutes(5))

        val result = CheckInValidator.validate(
            expectedCode = code,
            providedCode = code,
            startTime = startTime,
            endTime = endTime,
            now = now,
            requestLocation = location,
            volunteerLocation = null,
            currentStatus = ParticipationStatus.CANCELLED
        )

        assertTrue(result is CheckInResult.Rejected)
        assertEquals(CheckInRejection.NOT_SIGNED_UP, result.reason)
    }

    @Test
    fun `location beyond 150 meters is rejected as too far`() {
        val now = startTime.plus(Duration.ofMinutes(5))
        // 51.595 is ~1.5km away from 51.582
        val farLocation = Location(51.595, 4.771)

        val result = CheckInValidator.validate(
            expectedCode = code,
            providedCode = code,
            startTime = startTime,
            endTime = endTime,
            now = now,
            requestLocation = location,
            volunteerLocation = farLocation,
            currentStatus = ParticipationStatus.SIGNED_UP
        )

        assertTrue(result is CheckInResult.Rejected)
        assertEquals(CheckInRejection.LOCATION_TOO_FAR, result.reason)
    }
}
