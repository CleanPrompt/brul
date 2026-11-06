package nl.example.backend.features.authorization.model

/**
 * Every permission the application checks. Defined in code so a typo is a compile error;
 * [key] is the name stored in the `permissions` table.
 *
 * A new feature adds its permissions here and grants them in [DefaultRole].
 */
enum class Permission(val key: String) {
    USER_READ("user.read"),
    USER_UPDATE("user.update"),
    USER_DELETE("user.delete"),
    ROLE_READ("role.read"),
    ROLE_ASSIGN("role.assign"),
    FEEDBACK_READ("feedback.read"),
    FEEDBACK_UPDATE("feedback.update"),
    FEEDBACK_DELETE("feedback.delete");

    companion object {
        fun fromKey(key: String): Permission? = entries.find { it.key == key }
    }
}
