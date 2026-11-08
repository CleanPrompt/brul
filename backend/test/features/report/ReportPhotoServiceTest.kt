package nl.brul.backend.features.report

import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.service.ReportPhotoService
import nl.brul.backend.shared.error.AppException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The photo service with fakes: rules, GPS (G1, G2), storage and clean-up. */
class ReportPhotoServiceTest {
    private val owner = UUID.randomUUID()
    private val report = ReportTestData.nuisance(ownerId = owner)
    private val reports = FakeReportRepository().also { it.save(report) }
    private val photos = FakeReportPhotoRepository()
    private val storage = InMemoryPhotoStorage()
    private val service = ReportPhotoService(
        reports,
        photos,
        storage,
        Clock.fixed(Instant.parse("2026-11-08T09:30:00Z"), ZoneOffset.UTC)
    )

    private fun upload(bytes: ByteArray = ReportTestData.jpeg(), appLocation: Location? = null) =
        service.upload(report.id, owner, bytes, appLocation)

    @Test
    fun `G1 the location comes from the photo itself`() {
        val photo = upload(ReportTestData.jpegWithGps(51.5856, 4.7763))

        assertTrue(abs(assertNotNull(photo.latitude) - 51.5856) < 0.00001)
        assertTrue(abs(assertNotNull(photo.longitude) - 4.7763) < 0.00001)
        assertEquals("PHOTO", photo.locationSource)
    }

    @Test
    fun `G1 the photo wins over the app location`() {
        val photo = upload(ReportTestData.jpegWithGps(51.5856, 4.7763), appLocation = Location(52.0, 5.0))

        assertEquals("PHOTO", photo.locationSource)
    }

    @Test
    fun `G2 without GPS in the photo the app location is used`() {
        val photo = upload(appLocation = Location(52.0, 5.0))

        assertEquals(52.0, photo.latitude)
        assertEquals("APP", photo.locationSource)
    }

    @Test
    fun `without any GPS the photo has no location`() {
        val photo = upload(ReportTestData.png())

        assertNull(photo.latitude)
        assertNull(photo.locationSource)
    }

    @Test
    fun `the file is stored under the photo id and the server sets the time and the url`() {
        val photo = upload()

        assertEquals(setOf("${photo.id}.jpg"), storage.files.keys)
        assertEquals("image/jpeg", photo.contentType)
        assertEquals(owner.toString(), photo.uploaderId)
        assertEquals("2026-11-08T09:30:00Z", photo.createdAt)
        assertEquals("/api/v1/photos/${photo.id}", photo.url)
    }

    @Test
    fun `an unknown report gives 404`() {
        val error = assertFailsWith<AppException.NotFound> {
            service.upload(UUID.randomUUID(), owner, ReportTestData.jpeg(), null)
        }

        assertEquals("REPORT_NOT_FOUND", error.code)
        assertFailsWith<AppException.NotFound> { service.listByReport(UUID.randomUUID()) }
        assertFailsWith<AppException.NotFound> { service.getReportOwnerId(UUID.randomUUID()) }
    }

    @Test
    fun `P3 a text file is rejected and nothing is stored`() {
        assertFailsWith<AppException.UnsupportedMediaType> { upload("not an image".toByteArray()) }

        assertTrue(storage.files.isEmpty())
        assertTrue(photos.photos.isEmpty())
    }

    @Test
    fun `P4 the fourth photo is rejected`() {
        repeat(3) { upload() }

        assertFailsWith<AppException.Conflict> { upload() }
        assertEquals(3, storage.files.size)
    }

    @Test
    fun `no file is left behind when saving the data fails`() {
        photos.failOnSave = true

        assertFailsWith<IllegalStateException> { upload() }

        assertTrue(storage.files.isEmpty())
    }

    @Test
    fun `the list shows the photos of the report, oldest first, and the image comes back`() {
        val first = upload()
        val bytes = ReportTestData.png()
        val second = upload(bytes)

        assertEquals(listOf(first.id, second.id), service.listByReport(report.id).map { it.id })

        val image = service.getImage(UUID.fromString(second.id))
        assertEquals("image/png", image.contentType)
        assertTrue(bytes.contentEquals(image.bytes))
    }

    @Test
    fun `delete removes the data and the file`() {
        val id = UUID.fromString(upload().id)

        assertEquals(owner, service.getUploaderId(id))
        service.delete(id)

        assertTrue(storage.files.isEmpty())
        val error = assertFailsWith<AppException.NotFound> { service.getImage(id) }
        assertEquals("PHOTO_NOT_FOUND", error.code)
        assertFailsWith<AppException.NotFound> { service.delete(id) }
    }

    @Test
    fun `the files of a report can be cleaned up`() {
        upload()
        upload(ReportTestData.png())

        service.deleteFiles(service.storageKeysOf(report.id))

        assertTrue(storage.files.isEmpty())
    }
}
