package nl.brul.backend.features.help

import nl.brul.backend.features.help.model.ParticipationStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticipationStatusTest {

    @Test
    fun `signed up can transition to attended, no show, cancelled, or late cancelled`() {
        val status = ParticipationStatus.SIGNED_UP

        assertTrue(status.canTransitionTo(ParticipationStatus.ATTENDED))
        assertTrue(status.canTransitionTo(ParticipationStatus.NO_SHOW))
        assertTrue(status.canTransitionTo(ParticipationStatus.CANCELLED))
        assertTrue(status.canTransitionTo(ParticipationStatus.LATE_CANCELLED))
    }

    @Test
    fun `attended is a terminal state and cannot transition anywhere`() {
        val status = ParticipationStatus.ATTENDED

        ParticipationStatus.entries.forEach { next ->
            assertFalse(status.canTransitionTo(next))
        }
    }

    @Test
    fun `no show is a terminal state and cannot transition anywhere`() {
        val status = ParticipationStatus.NO_SHOW

        ParticipationStatus.entries.forEach { next ->
            assertFalse(status.canTransitionTo(next))
        }
    }

    @Test
    fun `cancelled and late cancelled cannot transition anywhere`() {
        ParticipationStatus.entries.forEach { next ->
            assertFalse(ParticipationStatus.CANCELLED.canTransitionTo(next))
            assertFalse(ParticipationStatus.LATE_CANCELLED.canTransitionTo(next))
        }
    }
}
