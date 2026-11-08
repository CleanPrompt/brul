package nl.brul.backend.features.report.model

/** What a report is about. Which categories a report may have depends on its [ReportType]. */
enum class ReportCategory {
    THEFT,
    SUSPICIOUS_SITUATION,
    TRAFFIC_SAFETY,
    NOISE,
    WASTE,
    VANDALISM,
    PARKING,
    GROCERIES,
    TRANSPORT,
    MOVING,
    CHORES,
    OTHER
}
