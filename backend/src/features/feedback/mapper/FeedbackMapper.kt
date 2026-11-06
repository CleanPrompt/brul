package nl.example.backend.features.feedback.mapper

import nl.example.backend.features.feedback.dto.FeedbackResponse
import nl.example.backend.features.feedback.model.Feedback

fun Feedback.toResponse(): FeedbackResponse =
    FeedbackResponse(
        id = id.toString(),
        userId = userId.toString(),
        message = message,
        status = status.name,
        createdAt = createdAt.toString()
    )
