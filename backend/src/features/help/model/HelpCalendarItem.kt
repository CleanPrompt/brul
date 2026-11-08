package nl.brul.backend.features.help.model

import java.util.UUID

/**
 * Data needed to generate an iCalendar (.ics) appointment for a help request.
 * Decouples the calendar generation from the report entity.
 */
data class HelpCalendarItem(
    val id: UUID,
    val title: String,
    val description: String,
    val category: String,
    val location: Location
)
