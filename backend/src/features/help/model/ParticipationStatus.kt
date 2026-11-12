package nl.brul.backend.features.help.model

/**
 * State machine representing the lifecycle of a volunteer's participation in a help request.
 */
enum class ParticipationStatus {
    SIGNED_UP,
    ATTENDED,
    NO_SHOW,
    CANCELLED,
    LATE_CANCELLED;

    /**
     * Determines whether transitioning from the current state to [next] is permitted by domain rules.
     */
    fun canTransitionTo(next: ParticipationStatus): Boolean = when (this) {
        SIGNED_UP -> next in setOf(ATTENDED, NO_SHOW, CANCELLED, LATE_CANCELLED)
        CANCELLED, LATE_CANCELLED, ATTENDED, NO_SHOW -> false
    }
}
