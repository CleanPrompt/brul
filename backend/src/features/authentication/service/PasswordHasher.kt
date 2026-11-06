package nl.brul.backend.features.authentication.service

import org.mindrot.jbcrypt.BCrypt

class PasswordHasher {

    fun hash(password: String): String =
        BCrypt.hashpw(password, BCrypt.gensalt(12))

    fun verify(password: String, passwordHash: String): Boolean =
        try {
            BCrypt.checkpw(password, passwordHash)
        } catch (_: IllegalArgumentException) {
            false
        }
}