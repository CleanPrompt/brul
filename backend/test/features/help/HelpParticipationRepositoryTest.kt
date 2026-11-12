package nl.brul.backend.features.help

import nl.brul.backend.TEST_CREATED_AT
import nl.brul.backend.features.help.database.HelpParticipationsTable
import nl.brul.backend.features.help.model.HelpParticipation
import nl.brul.backend.features.help.model.ParticipationStatus
import nl.brul.backend.features.help.repository.ExposedHelpParticipationRepository
import nl.brul.backend.features.user.database.UsersTable
import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.repository.ExposedUserRepository
import nl.brul.backend.shared.error.AppException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HelpParticipationRepositoryTest {

    private val repository = ExposedHelpParticipationRepository()
    private val userRepository = ExposedUserRepository()

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:help_participation_repository_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(UsersTable, HelpParticipationsTable) }
    }

    private fun newUser(): UUID =
        userRepository.save(
            User(UUID.randomUUID(), "${UUID.randomUUID()}@example.com", "Volunteer", "hash", true, TEST_CREATED_AT)
        ).id

    @Test
    fun `saves and retrieves a participation`() {
        val userId = newUser()
        val helpRequestId = UUID.randomUUID()
        val participation = HelpParticipation(
            id = UUID.randomUUID(),
            helpRequestId = helpRequestId,
            userId = userId,
            status = ParticipationStatus.SIGNED_UP
        )

        val saved = repository.save(participation)

        assertEquals(participation, repository.findById(saved.id))
    }

    @Test
    fun `findByHelpRequestAndUser returns matching participation`() {
        val userId = newUser()
        val helpRequestId = UUID.randomUUID()
        val participation = repository.save(
            HelpParticipation(
                id = UUID.randomUUID(),
                helpRequestId = helpRequestId,
                userId = userId,
                status = ParticipationStatus.SIGNED_UP
            )
        )

        val found = repository.findByHelpRequestAndUser(helpRequestId, userId)

        assertNotNull(found)
        assertEquals(participation.id, found.id)
    }

    @Test
    fun `countByHelpRequest counts participations accurately`() {
        val helpRequestId = UUID.randomUUID()
        val user1 = newUser()
        val user2 = newUser()

        assertEquals(0, repository.countByHelpRequest(helpRequestId))

        repository.save(HelpParticipation(UUID.randomUUID(), helpRequestId, user1))
        repository.save(HelpParticipation(UUID.randomUUID(), helpRequestId, user2))

        assertEquals(2, repository.countByHelpRequest(helpRequestId))
    }

    @Test
    fun `updateStatus updates status and check-in timestamp`() {
        val userId = newUser()
        val helpRequestId = UUID.randomUUID()
        repository.save(HelpParticipation(UUID.randomUUID(), helpRequestId, userId))

        val checkInTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val updated = repository.updateStatus(
            helpRequestId = helpRequestId,
            userId = userId,
            status = ParticipationStatus.ATTENDED,
            checkedInAt = checkInTime
        )

        assertTrue(updated)
        val loaded = repository.findByHelpRequestAndUser(helpRequestId, userId)
        assertNotNull(loaded)
        assertEquals(ParticipationStatus.ATTENDED, loaded.status)
        assertEquals(checkInTime, loaded.checkedInAt)
    }

    @Test
    fun `delete removes the participation`() {
        val userId = newUser()
        val helpRequestId = UUID.randomUUID()
        repository.save(HelpParticipation(UUID.randomUUID(), helpRequestId, userId))

        val removed = repository.delete(helpRequestId, userId)

        assertTrue(removed)
        assertNull(repository.findByHelpRequestAndUser(helpRequestId, userId))
        assertFalse(repository.delete(helpRequestId, userId))
    }

    @Test
    fun `duplicate save for same help request and user throws Conflict`() {
        val userId = newUser()
        val helpRequestId = UUID.randomUUID()
        repository.save(HelpParticipation(UUID.randomUUID(), helpRequestId, userId))

        val duplicate = HelpParticipation(UUID.randomUUID(), helpRequestId, userId)

        assertFailsWith<AppException.Conflict> {
            repository.save(duplicate)
        }
    }
}
