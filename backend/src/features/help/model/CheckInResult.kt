package nl.brul.backend.features.help.model

/**
 * Result of evaluating a volunteer check-in attempt.
 */
sealed interface CheckInResult {
    data object Success : CheckInResult
    data class Rejected(val reason: CheckInRejection) : CheckInResult
}

enum class CheckInRejection {
    INVALID_CODE,
    TOO_EARLY,
    TOO_LATE,
    NOT_SIGNED_UP,
    ALREADY_ATTENDED,
    LOCATION_TOO_FAR
}
