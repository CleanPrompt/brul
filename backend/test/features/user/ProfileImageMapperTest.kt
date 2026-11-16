package nl.brul.backend.features.user

import io.ktor.http.ContentType
import nl.brul.backend.TEST_CREATED_AT
import nl.brul.backend.features.user.mapper.contentType
import nl.brul.backend.features.user.model.ProfileImage
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileImageMapperTest {
    @Test
    fun `the content type follows from the first bytes`() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x01)
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x02)

        assertEquals(ContentType.Image.JPEG, ProfileImage(UUID.randomUUID(), jpeg, TEST_CREATED_AT).contentType())
        assertEquals(ContentType.Image.PNG, ProfileImage(UUID.randomUUID(), png, TEST_CREATED_AT).contentType())
    }
}
