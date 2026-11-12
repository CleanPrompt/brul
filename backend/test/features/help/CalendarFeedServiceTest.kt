package nl.brul.backend.features.help

import nl.brul.backend.features.help.model.HelpCalendarItem
import nl.brul.backend.features.help.model.Location
import nl.brul.backend.features.help.service.CalendarFeedService
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertTrue

class CalendarFeedServiceTest {

    private val testId = UUID.fromString("00000000-0000-0000-0000-000000000042")

    @Test
    fun `generateIcs produces valid RFC 5545 format with CRLF and required fields`() {
        val item = HelpCalendarItem(
            id = testId,
            title = "Help met tuinieren",
            description = "Onkruid wieden en heg snoeien",
            category = "CHORES",
            location = Location(51.582, 4.771)
        )

        val start = Instant.parse("2026-11-20T10:00:00Z")
        val end = Instant.parse("2026-11-20T12:00:00Z")
        val now = Instant.parse("2026-11-15T08:00:00Z")

        val ics = CalendarFeedService.generateIcs(
            item = item,
            startTime = start,
            endTime = end,
            now = now
        )

        // RFC 5545 requires CRLF line endings
        assertTrue(ics.contains("\r\n"))

        // Standard iCalendar headers
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"))
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
        assertTrue(ics.contains("VERSION:2.0\r\n"))
        assertTrue(ics.contains("BEGIN:VEVENT\r\n"))

        // Event identifiers and timestamps
        assertTrue(ics.contains("UID:brul-help-$testId@brul.nl\r\n"))
        assertTrue(ics.contains("DTSTART:20261120T100000Z\r\n"))
        assertTrue(ics.contains("DTEND:20261120T120000Z\r\n"))
        assertTrue(ics.contains("DTSTAMP:20261115T080000Z\r\n"))

        // Content
        assertTrue(ics.contains("SUMMARY:Brul Hulp: Help met tuinieren\r\n"))
        assertTrue(ics.contains("LOCATION:51.582,4.771\r\n"))
        assertTrue(ics.contains("BEGIN:VALARM\r\n"))
        assertTrue(ics.contains("TRIGGER:-PT60M\r\n"))
    }

    @Test
    fun `generateIcs escapes special characters properly`() {
        val item = HelpCalendarItem(
            id = testId,
            title = "Klusje, snel; en simpel",
            description = "Lijn 1\nLijn 2",
            category = "OTHER",
            location = Location(51.582, 4.771)
        )

        val start = Instant.parse("2026-11-20T10:00:00Z")
        val end = Instant.parse("2026-11-20T12:00:00Z")

        val ics = CalendarFeedService.generateIcs(
            item = item,
            startTime = start,
            endTime = end
        )

        assertTrue(ics.contains("Klusje\\, snel\\; en simpel"))
        assertTrue(ics.contains("Lijn 1\\nLijn 2"))
    }
}
