package nl.brul.backend.config

import io.github.cdimascio.dotenv.dotenv

data class AppConfig(
    val host: String,
    val port: Int,
    val databaseUrl: String,
    val databaseDriver: String,
    val databaseUser: String,
    val databasePassword: String,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    /** The account that registers with this email becomes ADMIN; `null` disables it. */
    val bootstrapAdminEmail: String?
) {
    companion object {
        /** HMAC-SHA256 works with a 256-bit (32-byte) key; a shorter secret makes the token weaker than needed. */
        const val MIN_JWT_SECRET_LENGTH = 32

        fun load(): AppConfig {
            val dotenv = dotenv {
                directory = "backend"
                ignoreIfMissing = true
            }

            // Real environment variables take precedence over the local .env file.
            fun optional(name: String): String? = System.getenv(name) ?: dotenv[name]

            fun get(name: String, default: String? = null): String =
                optional(name) ?: default ?: error("Missing required configuration: $name")

            // HMAC256 needs a long secret; a short one can be guessed by trying many secrets against one token.
            // Checked here and not in the constructor, so tests can still build an AppConfig with a short secret.
            val jwtSecret = get("JWT_SECRET")
            if (jwtSecret.length < MIN_JWT_SECRET_LENGTH) {
                error("JWT_SECRET must contain at least $MIN_JWT_SECRET_LENGTH characters")
            }

            return AppConfig(
                host = get("HOST", "127.0.0.1"),
                port = get("PORT", "8080").toInt(),
                databaseUrl = get("DATABASE_URL", "jdbc:h2:file:./data/brul"),
                databaseDriver = get("DATABASE_DRIVER", "org.h2.Driver"),
                databaseUser = get("DATABASE_USER", "sa"),
                databasePassword = get("DATABASE_PASSWORD", ""),
                jwtSecret = jwtSecret,
                jwtIssuer = get("JWT_ISSUER", "brul-backend"),
                jwtAudience = get("JWT_AUDIENCE", "brul-api"),
                bootstrapAdminEmail = optional("BOOTSTRAP_ADMIN_EMAIL")
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf { it.isNotEmpty() }
            )
        }
    }
}
