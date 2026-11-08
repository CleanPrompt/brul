package nl.brul.backend.features.report.model

/** Where a report is in its handling. New reports start as [OPEN]; a [RESOLVED] report is final. */
enum class ReportStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED;

    /** The status rule: only forward (OPEN → IN_PROGRESS → RESOLVED, or OPEN → RESOLVED); no change is fine. */
    fun canChangeTo(next: ReportStatus): Boolean =
        next == this || when (this) {
            OPEN -> next == IN_PROGRESS || next == RESOLVED
            IN_PROGRESS -> next == RESOLVED
            RESOLVED -> false
        }
}
