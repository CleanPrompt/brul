package nl.brul.backend.features.help.service

import java.time.Instant
import java.util.UUID

/**
 * In-memory provider for help requests and check-in codes.
 * Serves as a standalone provider until the Report feature is fully integrated.
 */
class InMemoryHelpRequestProvider : HelpRequestProvider {
    val requests = mutableMapOf<UUID, HelpRequestInfo>()
    val checkInCodes = mutableMapOf<UUID, ActiveCheckInCode>()

    override fun findHelpRequest(id: UUID): HelpRequestInfo? = requests[id]

    override fun setCheckInCode(helpRequestId: UUID, code: String, expiresAt: Instant) {
        checkInCodes[helpRequestId] = ActiveCheckInCode(code, expiresAt)
    }

    override fun getCheckInCode(helpRequestId: UUID): ActiveCheckInCode? = checkInCodes[helpRequestId]
}
