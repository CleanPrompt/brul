package nl.brul.backend.features.report

import nl.brul.backend.TEST_CREATED_AT
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.ReportQuery
import nl.brul.backend.features.report.model.ReportSortField
import nl.brul.backend.features.report.service.ReportSearch
import nl.brul.backend.shared.paging.SortDirection
import kotlin.test.Test
import kotlin.test.assertEquals

/** The area filter and the sorting, as a pure function. */
class ReportSearchTest {
    private val centre = ReportTestData.nuisance(location = ReportTestData.BREDA)
    // About 1 km and about 22 km from the centre of Breda.
    private val nearby = ReportTestData.nuisance(
        location = Location(51.5950, 4.7800),
        createdAt = TEST_CREATED_AT.plusSeconds(60)
    )
    private val tilburg = ReportTestData.nuisance(
        location = Location(51.5555, 5.0913),
        createdAt = TEST_CREATED_AT.plusSeconds(120)
    )
    private val all = listOf(tilburg, centre, nearby)

    @Test
    fun `without an area every report stays, newest first by default`() {
        assertEquals(listOf(tilburg, nearby, centre), ReportSearch.select(all, ReportQuery()))
    }

    @Test
    fun `the radius keeps only the reports within that many kilometres`() {
        val query = ReportQuery(near = ReportTestData.BREDA, radiusKm = 2.0)

        assertEquals(setOf(centre, nearby), ReportSearch.select(all, query).toSet())
    }

    @Test
    fun `sorting on distance puts the nearest first`() {
        val query = ReportQuery(
            near = ReportTestData.BREDA,
            sortField = ReportSortField.DISTANCE,
            sortDirection = SortDirection.ASC
        )

        assertEquals(listOf(centre, nearby, tilburg), ReportSearch.select(all, query))
    }

    @Test
    fun `sorting on views puts the most viewed first`() {
        val popular = ReportTestData.nuisance(viewCount = 10)
        val quiet = ReportTestData.nuisance(viewCount = 1)

        val query = ReportQuery(sortField = ReportSortField.VIEW_COUNT)

        assertEquals(listOf(popular, quiet), ReportSearch.select(listOf(quiet, popular), query))
    }

    @Test
    fun `equal values keep a stable order on the id`() {
        val first = ReportTestData.nuisance()
        val second = ReportTestData.nuisance()
        val expected = listOf(first, second).sortedBy { it.id }

        assertEquals(expected, ReportSearch.select(listOf(second, first), ReportQuery()))
        assertEquals(expected, ReportSearch.select(listOf(first, second), ReportQuery()))
    }
}
