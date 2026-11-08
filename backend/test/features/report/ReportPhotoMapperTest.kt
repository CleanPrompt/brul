package nl.brul.backend.features.report

import nl.brul.backend.features.report.mapper.appLocationOf
import nl.brul.backend.features.report.mapper.toResponse
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.LocationSource
import nl.brul.backend.features.report.model.PhotoLocation
import nl.brul.backend.features.report.model.ReportPhoto
import nl.brul.backend.shared.error.AppException
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReportPhotoMapperTest {
    private fun photo(location: PhotoLocation?) = ReportPhoto(
        id = UUID.randomUUID(),
        reportId = UUID.randomUUID(),
        uploaderId = UUID.randomUUID(),
        contentType = "image/jpeg",
        sizeBytes = 10,
        storageKey = "key.jpg",
        createdAt = Instant.parse("2026-11-08T09:30:00Z"),
        location = location
    )

    @Test
    fun `the response has the location, its source, an ISO time and the image url`() {
        val photo = photo(PhotoLocation(Location(51.5, 4.6), LocationSource.APP))

        val response = photo.toResponse()

        assertEquals(51.5, response.latitude)
        assertEquals(4.6, response.longitude)
        assertEquals("APP", response.locationSource)
        assertEquals("2026-11-08T09:30:00Z", response.createdAt)
        assertEquals("/api/v1/photos/${photo.id}", response.url)
    }

    @Test
    fun `without a location all three location fields are empty`() {
        val response = photo(location = null).toResponse()

        assertNull(response.latitude)
        assertNull(response.longitude)
        assertNull(response.locationSource)
    }

    @Test
    fun `the app location needs both numbers or neither, on earth`() {
        assertEquals(Location(51.5, 4.6), appLocationOf(" 51.5 ", "4.6"))
        assertNull(appLocationOf(null, null))
        assertNull(appLocationOf("", " "))

        assertFailsWith<AppException.BadRequest> { appLocationOf("51.5", null) }
        assertFailsWith<AppException.BadRequest> { appLocationOf("north", "4.6") }
        assertFailsWith<AppException.BadRequest> { appLocationOf("91", "4.6") }
    }
}
