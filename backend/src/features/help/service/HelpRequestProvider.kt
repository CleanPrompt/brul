package nl.brul.backend.features.help.service

import nl.brul.backend.features.help.model.Location
import java.time.Instant
import java.util.UUID

/**
 * Summary data representing the minimum help request info needed by the help feature.
 */
data class HelpRequestInfo(
    val id: UUID,
    val ownerId: UUID,
    val title: String,
    val description: String,
    val category: String,
    val location: Location,
    val volunteersNeeded: Int,
    val isOpen: Boolean = true,
    val startTime: Instant,
    val endTime: Instant
)

/**
 * Active check-in code and expiration timestamp.
 */
data class ActiveCheckInCode(
    val code: String,
    val expiresAt: Instant
)

/**
 * Provider interface decoupling the help feature from the concrete Report repository and tables.
 */
interface HelpRequestProvider {
    fun findHelpRequest(id: UUID): HelpRequestInfo?
    fun setCheckInCode(helpRequestId: UUID, code: String, expiresAt: Instant)
    fun getCheckInCode(helpRequestId: UUID): ActiveCheckInCode?
}
