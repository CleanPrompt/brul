package nl.example.backend.features.user.service

import nl.example.backend.features.user.dto.UpdateUserRequest
import nl.example.backend.features.user.dto.UserResponse
import nl.example.backend.features.user.mapper.toResponse
import nl.example.backend.features.user.model.User
import nl.example.backend.features.user.model.UserQuery
import nl.example.backend.features.user.repository.UserRepository
import nl.example.backend.shared.error.AppException
import nl.example.backend.shared.error.requireValid
import nl.example.backend.shared.paging.PageResponse
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

    fun updateDisplayName(id: UUID, request: UpdateUserRequest): UserResponse {
        val name = request.displayName.trim()
        requireValid(name.isNotEmpty() && name.length <= 120) {
            "Display name must contain between 1 and 120 characters"
        }

        val updated = findUser(id).copy(displayName = name)
        repository.update(updated)
        return updated.toResponse()
    }

    /**
     * Deactivated users can no longer log in. Raising the token version keeps their old tokens invalid,
     * even if the account is reactivated later.
     */
    fun deactivate(id: UUID, currentUserId: UUID) {
        requireValid(id != currentUserId) { "You cannot deactivate your own account" }

        repository.update(findUser(id).copy(active = false))
        repository.incrementTokenVersion(id)
    }

    /**
     * Undoes [deactivate]. The token version stays as it is: tokens from before the deactivation remain invalid,
     * so the user has to log in again. Reactivating an active user changes nothing.
     */
    fun reactivate(id: UUID) {
        repository.update(findUser(id).copy(active = true))
    }

    private fun findUser(id: UUID): User =
        repository.findById(id) ?: throw AppException.NotFound("User not found", "USER_NOT_FOUND")
}
