package nl.brul.backend.features.report.model

import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.error.requireValid

/**
 * What differs per [ReportType]. A safety report says whether it is urgent, a help request how many volunteers it
 * needs, and a nuisance report has nothing extra. The three subtypes are kept together in this file because a sealed
 * interface and its implementations belong together.
 */
sealed interface ReportDetails {
    val type: ReportType

    data object Nuisance : ReportDetails {
        override val type = ReportType.NUISANCE
    }

    data class Safety(val urgent: Boolean) : ReportDetails {
        override val type = ReportType.SAFETY
    }

    data class HelpRequest(val volunteersNeeded: Int) : ReportDetails {
        override val type = ReportType.HELP_REQUEST

        init {
            requireValid(volunteersNeeded in 1..MAX_VOLUNTEERS) {
                "volunteersNeeded must be between 1 and $MAX_VOLUNTEERS"
            }
        }
    }

    companion object {
        const val MAX_VOLUNTEERS = 50

        /**
         * The details for [type] from the two optional request fields. A field that does not belong to the type is a
         * 400, so a client notices its mistake instead of the value being ignored.
         */
        fun of(type: ReportType, urgent: Boolean?, volunteersNeeded: Int?): ReportDetails =
            when (type) {
                ReportType.NUISANCE -> {
                    requireValid(urgent == null && volunteersNeeded == null) {
                        "A nuisance report has no urgent or volunteersNeeded"
                    }
                    Nuisance
                }

                ReportType.SAFETY -> {
                    requireValid(volunteersNeeded == null) { "A safety report has no volunteersNeeded" }
                    Safety(urgent ?: throw AppException.BadRequest("A safety report needs urgent (true or false)"))
                }

                ReportType.HELP_REQUEST -> {
                    requireValid(urgent == null) { "A help request has no urgent" }
                    HelpRequest(
                        volunteersNeeded ?: throw AppException.BadRequest("A help request needs volunteersNeeded")
                    )
                }
            }
    }
}
