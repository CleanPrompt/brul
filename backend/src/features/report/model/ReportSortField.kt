package nl.brul.backend.features.report.model

/** Fields a report list can be sorted on; [key] is the value of the `sort` query parameter. */
enum class ReportSortField(val key: String) {
    CREATED_AT("createdAt"),
    VIEW_COUNT("viewCount"),

    /** Distance to the `latitude` and `longitude` of the search; nearest first by default. */
    DISTANCE("distance")
}
