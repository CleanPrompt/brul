package nl.brul.backend.features.authorization.service

import nl.brul.backend.features.authorization.model.DefaultRole
import nl.brul.backend.features.authorization.model.Permission
import nl.brul.backend.features.authorization.model.Role
import nl.brul.backend.features.authorization.repository.AuthorizationRepository
import java.util.UUID

/**
 * Creates the permissions and default roles at startup. Safe to run every time: it only adds what is
 * missing. It never removes a permission from a role, so revoke those by hand if the matrix shrinks.
 */
class AuthorizationSeeder(private val repository: AuthorizationRepository) {

    fun seed() {
        Permission.entries.forEach { repository.createPermissionIfMissing(it) }

        DefaultRole.entries.forEach { defaultRole ->
            val role = repository.findRoleByName(defaultRole.name)
                ?: repository.createRole(Role(UUID.randomUUID(), defaultRole.name))

            defaultRole.permissions.forEach { repository.grantPermissionToRole(role.id, it) }
        }
    }
}
