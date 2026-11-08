package nl.brul.backend.features.report

import nl.brul.backend.features.report.model.ImageFormat
import nl.brul.backend.features.report.service.PhotoRules
import nl.brul.backend.shared.error.AppException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pure tests of the photo rules P2-P4. */
class PhotoRulesTest {
    private fun check(size: Long = 1_000, format: ImageFormat? = ImageFormat.JPEG, existing: Long = 0) =
        PhotoRules.check(size, format, existing)

    @Test
    fun `a normal JPEG or PNG is allowed and its format is returned`() {
        assertEquals(ImageFormat.JPEG, check())
        assertEquals(ImageFormat.PNG, check(format = ImageFormat.PNG))
    }

    @Test
    fun `P2 exactly 5 MB is allowed, one byte more is not`() {
        check(size = PhotoRules.MAX_PHOTO_BYTES)

        val error = assertFailsWith<AppException.PayloadTooLarge> { check(size = PhotoRules.MAX_PHOTO_BYTES + 1) }
        assertEquals("PHOTO_TOO_LARGE", error.code)
    }

    @Test
    fun `P3 an unknown format is rejected`() {
        val error = assertFailsWith<AppException.UnsupportedMediaType> { check(format = null) }
        assertEquals("UNSUPPORTED_PHOTO_TYPE", error.code)
    }

    @Test
    fun `P4 the fourth photo is rejected`() {
        check(existing = 2)

        val error = assertFailsWith<AppException.Conflict> { check(existing = 3) }
        assertEquals("PHOTO_LIMIT_REACHED", error.code)
    }

    @Test
    fun `the first broken rule decides`() {
        assertFailsWith<AppException.PayloadTooLarge> {
            check(size = PhotoRules.MAX_PHOTO_BYTES + 1, format = null, existing = 3)
        }
        assertFailsWith<AppException.UnsupportedMediaType> { check(format = null, existing = 3) }
    }

    @Test
    fun `a JPEG and a PNG are recognised by their bytes, other files are not`() {
        assertEquals(ImageFormat.JPEG, ImageFormat.detect(ReportTestData.jpeg()))
        assertEquals(ImageFormat.PNG, ImageFormat.detect(ReportTestData.png()))
        assertEquals(null, ImageFormat.detect("%PDF-1.7".toByteArray()))
        assertEquals(null, ImageFormat.detect(ByteArray(0)))
    }
}
