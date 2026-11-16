package nl.brul.backend.features.user

import nl.brul.backend.features.user.service.ProfileImageService
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.image.ImageType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ProfileImageServiceTest {
    private val repository = FakeProfileImageRepository()
    private val service = ProfileImageService(repository)
    private val userId = UUID.randomUUID()

    // Not real photos: only the first bytes matter.
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x01)
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x02)

    @Test
    fun `uploads an image and returns it`() {
        service.upload(userId, jpeg)

        val image = service.get(userId)

        assertEquals(userId, image.userId)
        assertContentEquals(jpeg, image.data)
        assertEquals(0, image.createdAt.nano, "createdAt must be whole seconds")
    }

    @Test
    fun `a new upload replaces the old image`() {
        service.upload(userId, jpeg)
        service.upload(userId, png)

        assertContentEquals(png, service.get(userId).data)
    }

    @Test
    fun `rejects a file that is not a JPEG or PNG and stores nothing`() {
        val error = assertFailsWith<AppException.BadRequest> { service.upload(userId, "hello".toByteArray()) }

        assertEquals("INVALID_IMAGE_TYPE", error.code)
        assertNull(repository.findByUserId(userId))
    }

    @Test
    fun `rejects an image over 2 MB and stores nothing`() {
        val tooLarge = jpeg.copyOf(ImageType.MAX_BYTES + 1)

        val error = assertFailsWith<AppException.BadRequest> { service.upload(userId, tooLarge) }

        assertEquals("IMAGE_TOO_LARGE", error.code)
        assertNull(repository.findByUserId(userId))
    }

    @Test
    fun `get returns 404 when the user has no image`() {
        val error = assertFailsWith<AppException.NotFound> { service.get(userId) }

        assertEquals("PROFILE_IMAGE_NOT_FOUND", error.code)
    }

    @Test
    fun `delete removes the image and returns 404 the second time`() {
        service.upload(userId, jpeg)

        service.delete(userId)

        assertNull(repository.findByUserId(userId))
        val error = assertFailsWith<AppException.NotFound> { service.delete(userId) }
        assertEquals("PROFILE_IMAGE_NOT_FOUND", error.code)
    }
}
