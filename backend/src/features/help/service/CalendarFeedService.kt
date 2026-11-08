package nl.brul.backend.features.help.service

import nl.brul.backend.features.help.model.HelpCalendarItem
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Service that exports help requests to RFC 5545 compliant iCalendar (.ics) format.
 */
object CalendarFeedService {

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        .withZone(ZoneOffset.UTC)

    fun generateIcs(
        item: HelpCalendarItem,
        startTime: Instant,
        endTime: Instant,
        now: Instant = Instant.now()
    ): String {
        val nowFormatted = formatter.format(now)
        val startFormatted = formatter.format(startTime)
        val endFormatted = formatter.format(endTime)

        val cleanTitle = item.title.escapeIcs()
        val cleanDesc = item.description.escapeIcs()

        return buildString {
            append("BEGIN:VCALENDAR\r\n")
            append("VERSION:2.0\r\n")
            append("PRODID:-//Brul//Buurthulp//NL\r\n")
            append("CALSCALE:GREGORIAN\r\n")
            append("METHOD:PUBLISH\r\n")
            append("BEGIN:VEVENT\r\n")
            append("UID:brul-help-${item.id}@brul.nl\r\n")
            append("DTSTAMP:$nowFormatted\r\n")
            append("DTSTART:$startFormatted\r\n")
            append("DTEND:$endFormatted\r\n")
            append("SUMMARY:Brul Hulp: $cleanTitle\r\n")
            append("DESCRIPTION:$cleanDesc\\n\\nCategorie: ${item.category}\r\n")
            append("LOCATION:${item.location.latitude},${item.location.longitude}\r\n")
            append("STATUS:CONFIRMED\r\n")
            append("BEGIN:VALARM\r\n")
            append("TRIGGER:-PT60M\r\n")
            append("ACTION:DISPLAY\r\n")
            append("DESCRIPTION:Herinnering: Je vrijwilligerstaak begint over 1 uur!\r\n")
            append("END:VALARM\r\n")
            append("END:VEVENT\r\n")
            append("END:VCALENDAR\r\n")
        }
    }

    private fun String.escapeIcs(): String =
        this.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
}
