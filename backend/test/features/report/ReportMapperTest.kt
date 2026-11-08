package nl.brul.backend.features.report

import io.ktor.http.parametersOf
import nl.brul.backend.features.report.mapper.toReportQuery
import nl.brul.backend.features.report.mapper.toResponse
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.ReportCategory
import nl.brul.backend.features.report.model.ReportDetails
import nl.brul.backend.features.report.model.ReportSortField
import nl.brul.backend.features.report.model.ReportType
import nl.brul.backend.shared.error.AppException
import nl.brul.backend.shared.paging.SortDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReportMapperTest {

    @Test
    fun `the response has the ids as text, the time in ISO 8601 and only the extra field of the type`() {
        val report = ReportTestData.nuisance().copy(
            category = ReportCategory.THEFT,
            details = ReportDetails.Safety(urgent = true)
        )

        val response = report.toResponse()

        assertEquals(report.id.toString(), response.id)
        assertEquals("SAFETY", response.type)
        assertEquals(true, response.urgent)
        assertNull(response.volunteersNeeded)
        assertEquals("2026-01-01T12:00:00Z", response.createdAt)
    }

    @Test
    fun `query parameters become a ReportQuery`() {
        val query = parametersOf(
            "q" to listOf("bike"),
            "type" to listOf("SAFETY"),
            "status" to listOf("OPEN"),
            "latitude" to listOf("51.58"),
            "longitude" to listOf("4.77"),
            "radiusKm" to listOf("2")
        ).toReportQuery()

        assertEquals("bike", query.text)
        assertEquals(ReportType.SAFETY, query.type)
        assertEquals(Location(51.58, 4.77), query.near)
        assertEquals(2.0, query.radiusKm)
        assertEquals(ReportSortField.CREATED_AT, query.sortField)
        assertEquals(SortDirection.DESC, query.sortDirection)
    }

    @Test
    fun `sorting on distance is nearest first unless an order is given`() {
        val nearestFirst = parametersOf(
            "sort" to listOf("distance"),
            "latitude" to listOf("51.58"),
            "longitude" to listOf("4.77")
        ).toReportQuery()

        assertEquals(SortDirection.ASC, nearestFirst.sortDirection)
    }

    @Test
    fun `an area or distance without a location, or half a location, is a 400`() {
        assertFailsWith<AppException.BadRequest> { parametersOf("radiusKm", "2").toReportQuery() }
        assertFailsWith<AppException.BadRequest> { parametersOf("sort", "distance").toReportQuery() }
        assertFailsWith<AppException.BadRequest> { parametersOf("latitude", "51.58").toReportQuery() }
        assertFailsWith<AppException.BadRequest> { parametersOf("type", "FIRE").toReportQuery() }
        assertFailsWith<AppException.BadRequest> {
            parametersOf("latitude" to listOf("51.58"), "longitude" to listOf("4.77"), "radiusKm" to listOf("500"))
                .toReportQuery()
        }
    }
}
