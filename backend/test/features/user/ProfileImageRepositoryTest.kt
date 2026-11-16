package nl.brul.backend.features.user

import nl.brul.backend.TEST_CREATED_AT
import nl.brul.backend.features.user.database.UserProfileImagesTable
import nl.brul.backend.features.user.database.UsersTable
import nl.brul.backend.features.user.model.ProfileImage
import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.repository.ExposedProfileImageRepository
import nl.brul.backend.features.user.repository.ExposedUserRepository
import nl.brul.backend.shared.image.ImageType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileImageRepositoryTest {
    private val repository = ExposedProfileImageRepository()
    private val userRepository = ExposedUserRepository()

    // Not real photos: only the first bytes matter.
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x01)
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x02)

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:profile_image_repository_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(UsersTable, UserProfileImagesTable) }
    }

    /** The foreign key needs an existing user. The database is shared by these tests, so each gets a new one. */
    private fun newUser(): UUID =
        userRepository.save(
            User(UUID.randomUUID(), "${UUID.randomUUID()}@example.com", "Owner", "hash", true, TEST_CREATED_AT)
        ).id

    @Test
    fun `saves and finds an image`() {
        val userId = newUser()

        repository.save(ProfileImage(userId, jpeg, TEST_CREATED_AT))
        val found = assertNotNull(repository.findByUserId(userId))

        // A ByteArray is compared on its content with assertContentEquals, not with assertEquals.
        assertContentEquals(jpeg, found.data)
        assertEquals(TEST_CREATED_AT, found.createdAt)
    }

    @Test
    fun `a second save replaces the first image`() {
        val userId = newUser()

        repository.save(ProfileImage(userId, jpeg, TEST_CREATED_AT))
        repository.save(ProfileImage(userId, png, TEST_CREATED_AT.plusSeconds(60)))
        val found = assertNotNull(repository.findByUserId(userId))

        assertContentEquals(png, found.data)
        assertEquals(TEST_CREATED_AT.plusSeconds(60), found.createdAt)
    }

    @Test
    fun `finds nothing for a user without an image`() {
        assertNull(repository.findByUserId(newUser()))
    }

    @Test
    fun `delete removes the image and returns false the second time`() {
        val userId = newUser()
        repository.save(ProfileImage(userId, jpeg, TEST_CREATED_AT))

        assertTrue(repository.delete(userId))
        assertNull(repository.findByUserId(userId))
        assertFalse(repository.delete(userId))
    }

    @Test
    fun `an image of the maximum size fits in the column`() {
        val userId = newUser()
        // copyOf keeps the JPEG start and fills the rest with zeros.
        val largest = jpeg.copyOf(ImageType.MAX_BYTES)

        repository.save(ProfileImage(userId, largest, TEST_CREATED_AT))

        assertContentEquals(largest, assertNotNull(repository.findByUserId(userId)).data)
    }
}
