package nl.brul.backend.features.help

import nl.brul.backend.features.help.service.ActiveCheckInCode
import nl.brul.backend.features.help.service.HelpRequestInfo
import nl.brul.backend.features.help.service.HelpRequestProvider
import java.time.Instant
import java.util.UUID

/**
 * In-memory fake provider for help requests and check-in codes.
 */
class FakeHelpRequestProvider : HelpRequestProvider {
    val requests = mutableMapOf<UUID, HelpRequestInfo>()
    val checkInCodes = mutableMapOf<UUID, ActiveCheckInCode>()

    override fun findHelpRequest(id: UUID): HelpRequestInfo? = requests[id]

    override fun setCheckInCode(helpRequestId: UUID, code: String, expiresAt: Instant) {
        checkInCodes[helpRequestId] = ActiveCheckInCode(code, expiresAt)
    }

    override fun getCheckInCode(helpRequestId: UUID): ActiveCheckInCode? = checkInCodes[helpRequestId]

    fun clear() {
        requests.clear()
        checkInCodes.clear()
    }
}
