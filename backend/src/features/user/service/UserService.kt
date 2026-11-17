package nl.brul.backend.features.user.service

import nl.brul.backend.features.user.dto.UpdateUserRequest
import nl.brul.backend.features.user.dto.UserResponse
import nl.brul.backend.features.user.mapper.toResponse
import nl.brul.backend.features.user.model.User
import nl.brul.backend.features.user.model.UserQuery
import nl.brul.backend.features.user.repository.UserRepository
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.error.requireValid
import nl.brul.backend.shared.paging.PageResponse
import java.util.UUID

class UserService(private val repository: UserRepository) {
    /** One page of matching users, plus the total number of matches. */
    fun search(query: UserQuery): PageResponse<UserResponse> =
        PageResponse(
            items = repository.search(query).map { it.toResponse() },
            total = repository.count(query),
            limit = query.page.limit,
            offset = query.page.offset
        )

    fun getById(id: UUID): UserResponse =
        findUser(id).toResponse()

    /**
     * Users own their account, so the owner of an account is the account itself. Like `getOwnerId` in other
     * features, it reads the user first, so an unknown id gives 404 before the access check.
     */
    fun getOwnerId(id: UUID): UUID =
        findUser(id).id

    fun updateDisplayName(id: UUID, request: UpdateUserRequest): UserResponse {
        val name = request.displayName.trim()
        requireValid(name.isNotEmpty() && name.length <= 120) {
            "Display name must contain between 1 and 120 characters"
        }

        // Read first for the 404; the update itself writes only the display name.
        val user = findUser(id)
        repository.updateDisplayName(id, name)
        return user.copy(displayName = name).toResponse()
    }

    /**
     * Deactivated users can no longer log in. Raising the token version keeps their old tokens invalid,
     * even if the account is reactivated later.
     */
    fun deactivate(id: UUID, currentUserId: UUID) {
        requireValid(id != currentUserId) { "You cannot deactivate your own account" }

        findUser(id)
        repository.setActive(id, false)
        repository.incrementTokenVersion(id)
    }

    /**
     * Undoes [deactivate]. The token version stays as it is: tokens from before the deactivation remain invalid,
     * so the user has to log in again. Reactivating an active user changes nothing.
     */
    fun reactivate(id: UUID) {
        findUser(id)
        repository.setActive(id, true)
    }

    private fun findUser(id: UUID): User =
        repository.findById(id) ?: throw AppException.NotFound("User not found", "USER_NOT_FOUND")
}
