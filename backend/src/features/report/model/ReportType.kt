package nl.brul.backend.features.report.model

/** The three kinds of report, each with the [categories] it may use. */
enum class ReportType(val categories: Set<ReportCategory>) {
    /** Something unsafe in the neighbourhood; the reporter says whether it is urgent. */
    SAFETY(setOf(ReportCategory.THEFT, ReportCategory.SUSPICIOUS_SITUATION, ReportCategory.TRAFFIC_SAFETY)),

    /** Something annoying, such as noise or rubbish. */
    NUISANCE(setOf(ReportCategory.NOISE, ReportCategory.WASTE, ReportCategory.VANDALISM, ReportCategory.PARKING)),

    /** A neighbour asks for help; the reporter says how many volunteers are needed. */
    HELP_REQUEST(
        setOf(
            ReportCategory.GROCERIES,
            ReportCategory.TRANSPORT,
            ReportCategory.MOVING,
            ReportCategory.CHORES,
            ReportCategory.OTHER
        )
    )
}
