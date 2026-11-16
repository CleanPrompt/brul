package nl.brul.backend.shared.image

import nl.brul.backend.shared.error.AppException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ImageTypeTest {
    // Not real photos: only the first bytes matter, so a signature plus one zero is enough.
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x00)

    @Test
    fun `detects a JPEG and a PNG`() {
        assertEquals(ImageType.JPEG, ImageType.detect(jpeg))
        assertEquals(ImageType.PNG, ImageType.detect(png))
    }

    @Test
    fun `returns null for text, a GIF, empty bytes and bytes that are too short`() {
        assertNull(ImageType.detect("hello".toByteArray()))
        assertNull(ImageType.detect("GIF89a".toByteArray()))
        assertNull(ImageType.detect(ByteArray(0)))
        assertNull(ImageType.detect(byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
    }

    @Test
    fun `accepts an image of exactly 2 MB`() {
        // copyOf keeps the JPEG start and fills the rest with zeros.
        val image = jpeg.copyOf(ImageType.MAX_BYTES)

        assertEquals(ImageType.JPEG, requireValidImage(image))
    }

    @Test
    fun `rejects one byte more than 2 MB with IMAGE_TOO_LARGE`() {
        val image = jpeg.copyOf(ImageType.MAX_BYTES + 1)

        val error = assertFailsWith<AppException.BadRequest> { requireValidImage(image) }

        assertEquals("IMAGE_TOO_LARGE", error.code)
    }

    @Test
    fun `rejects a file that is not an image with INVALID_IMAGE_TYPE`() {
        val error = assertFailsWith<AppException.BadRequest> { requireValidImage("hello".toByteArray()) }

        assertEquals("INVALID_IMAGE_TYPE", error.code)
    }
}
