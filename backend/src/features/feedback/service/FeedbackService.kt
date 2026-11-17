package nl.brul.backend.features.feedback.service

import nl.brul.backend.features.feedback.dto.FeedbackRequest
import nl.brul.backend.features.feedback.dto.FeedbackResponse
import nl.brul.backend.features.feedback.dto.UpdateFeedbackStatusRequest
import nl.brul.backend.features.feedback.mapper.toResponse
import nl.brul.backend.features.feedback.model.Feedback
import nl.brul.backend.features.feedback.model.FeedbackQuery
import nl.brul.backend.features.feedback.model.FeedbackStatus
import nl.brul.backend.features.feedback.repository.FeedbackRepository
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.error.requireValid
import nl.brul.backend.shared.paging.PageResponse
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Business rules for feedback. Access checks happen in the routes, through `AuthorizationService`. */
class FeedbackService(private val repository: FeedbackRepository) {
    /** The server sets the id, the owner ([ownerId]), the status and the creation time; the client only sends text. */
    fun create(ownerId: UUID, request: FeedbackRequest): FeedbackResponse {
        val feedback = Feedback(
            id = UUID.randomUUID(),
            ownerId = ownerId,
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
        findFeedback(id).ownerId

    fun updateMessage(id: UUID, request: FeedbackRequest): FeedbackResponse {
        val message = validMessage(request.message)
        // Read first for the 404; the update itself writes only the message.
        val feedback = findFeedback(id)
        repository.updateMessage(id, message)
        return feedback.copy(message = message).toResponse()
    }

    fun updateStatus(id: UUID, request: UpdateFeedbackStatusRequest): FeedbackResponse {
        val status = FeedbackStatus.entries.find { it.name == request.status }
            ?: throw AppException.BadRequest("Status must be one of: ${FeedbackStatus.entries.joinToString()}")

        val feedback = findFeedback(id)
        repository.updateStatus(id, status)
        return feedback.copy(status = status).toResponse()
    }

    /** A hard delete: unlike users, feedback items have no history worth keeping. */
    fun delete(id: UUID) {
        if (!repository.delete(id)) {
            throw feedbackNotFound()
        }
    }

    private fun validMessage(message: String): String {
        val trimmed = message.trim()
        requireValid(trimmed.isNotEmpty() && trimmed.length <= Feedback.MAX_MESSAGE_LENGTH) {
            "Message must contain between 1 and ${Feedback.MAX_MESSAGE_LENGTH} characters"
        }
        return trimmed
    }

    private fun findFeedback(id: UUID): Feedback =
        repository.findById(id) ?: throw feedbackNotFound()

    private fun feedbackNotFound() = AppException.NotFound("Feedback not found", "FEEDBACK_NOT_FOUND")
}
