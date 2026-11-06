package nl.brul.backend.features.user.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val active: Boolean,
    /** Set by the server at registration (UTC); never changed afterwards. */
    val createdAt: Instant,
    /** Copied into every token as the `ver` claim; raising it invalidates all earlier tokens. Never exposed. */
    val tokenVersion: Int = 0
)
