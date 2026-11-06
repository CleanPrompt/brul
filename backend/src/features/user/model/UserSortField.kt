package nl.example.backend.features.user.model

/** Fields a user list can be sorted on; [key] is the value of the `sort` query parameter. */
enum class UserSortField(val key: String) {
    CREATED_AT("createdAt"),
    DISPLAY_NAME("displayName"),
    EMAIL("email")
}
