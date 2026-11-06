package nl.example.backend.features.feedback.service

import nl.example.backend.features.feedback.dto.FeedbackRequest
import nl.example.backend.features.feedback.dto.FeedbackResponse
import nl.example.backend.features.feedback.dto.UpdateFeedbackStatusRequest
import nl.example.backend.features.feedback.mapper.toResponse
import nl.example.backend.features.feedback.model.Feedback
import nl.example.backend.features.feedback.model.FeedbackQuery
import nl.example.backend.features.feedback.model.FeedbackStatus
import nl.example.backend.features.feedback.repository.FeedbackRepository
import nl.example.backend.shared.error.AppException
import nl.example.backend.shared.error.requireValid
import nl.example.backend.shared.paging.PageResponse
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Business rules for feedback. Access checks happen in the routes, through `AuthorizationService`. */
class FeedbackService(private val repository: FeedbackRepository) {
    /** The server sets the id, the owner ([userId]), the status and the creation time; the client only sends text. */
    fun create(userId: UUID, request: FeedbackRequest): FeedbackResponse {
        val feedback = Feedback(
            id = UUID.randomUUID(),
            userId = userId,
            message = validMessage(request.message),
            status = FeedbackStatus.OPEN,
            createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        return repository.save(feedback).toResponse()
    }

    /** One page of matching items, plus the total number of matches. */
    fun search(query: FeedbackQuery): PageResponse<FeedbackResponse> =
        PageResponse(
            items = repository.search(query).map { it.toResponse() },
            total = repository.count(query),
            limit = query.page.limit,
            offset = query.page.offset
        )

    fun getById(id: UUID): FeedbackResponse =
        findFeedback(id).toResponse()

    /** The owner of an item, so a route can check access before it reads or changes the item. */
    fun getOwnerId(id: UUID): UUID =
        findFeedback(id).userId

    fun updateMessage(id: UUID, request: FeedbackRequest): FeedbackResponse {
        val updated = findFeedback(id).copy(message = validMessage(request.message))
        repository.update(updated)
        return updated.toResponse()
    }

    fun updateStatus(id: UUID, request: UpdateFeedbackStatusRequest): FeedbackResponse {
        val status = FeedbackStatus.entries.find { it.name == request.status }
            ?: throw AppException.BadRequest("Status must be one of: ${FeedbackStatus.entries.joinToString()}")

        val updated = findFeedback(id).copy(status = status)
        repository.update(updated)
        return updated.toResponse()
    }

    /** A hard delete: unlike users, feedback items have no history worth keeping. */
    fun delete(id: UUID) {
        if (!repository.delete(id)) {
            throw feedbackNotFound()
        }
    }

    private fun validMessage(message: String): String {
        val trimmed = message.trim()
        requireValid(trimmed.isNotEmpty() && trimmed.length <= MAX_MESSAGE_LENGTH) {
            "Message must contain between 1 and $MAX_MESSAGE_LENGTH characters"
        }
        return trimmed
    }

    private fun findFeedback(id: UUID): Feedback =
        repository.findById(id) ?: throw feedbackNotFound()

    private fun feedbackNotFound() = AppException.NotFound("Feedback not found", "FEEDBACK_NOT_FOUND")

    private companion object {
        /** Same as the column length in `FeedbackTable`. */
        const val MAX_MESSAGE_LENGTH = 2000
    }
}
