package nl.brul.backend.features.authorization.model

/**
 * Roles created at startup with the permissions they grant (the permission matrix).
 *
 * USER has no permissions: regular users may only access their own account and their own feedback,
 * which `AuthorizationService.requireOwnerOrPermission` allows without a permission.
 */
enum class DefaultRole(val permissions: Set<Permission>) {
    ADMIN(Permission.entries.toSet()),
    USER(emptySet())
}
