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
        fun load(): AppConfig {
            val dotenv = dotenv {
                directory = "backend"
                ignoreIfMissing = true
            }

            // Real environment variables take precedence over the local .env file.
            fun optional(name: String): String? = System.getenv(name) ?: dotenv[name]

            fun get(name: String, default: String? = null): String =
                optional(name) ?: default ?: error("Missing required configuration: $name")

            return AppConfig(
                host = get("HOST", "127.0.0.1"),
                port = get("PORT", "8080").toInt(),
                databaseUrl = get("DATABASE_URL", "jdbc:h2:file:./data/brul"),
                databaseDriver = get("DATABASE_DRIVER", "org.h2.Driver"),
                databaseUser = get("DATABASE_USER", "sa"),
                databasePassword = get("DATABASE_PASSWORD", ""),
                jwtSecret = get("JWT_SECRET"),
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
