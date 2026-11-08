package nl.brul.backend.features.report

import nl.brul.backend.features.report.model.Location
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationTest {
    @Test
    fun `accepts coordinate boundaries`() {
        Location(-90.0, -180.0)
        Location(90.0, 180.0)
        Location(0.0, 0.0)
    }

    @Test
    fun `rejects invalid latitude`() {
        for (latitude in listOf(-90.001, 90.001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { Location(latitude, 0.0) }
        }
    }

    @Test
    fun `rejects invalid longitude`() {
        for (longitude in listOf(-180.001, 180.001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { Location(0.0, longitude) }
        }
    }

    @Test
    fun `same location has zero distance`() {
        val location = Location(51.59, 4.78)
        assertEquals(0.0, location.distanceTo(location), 0.000001)
    }

    @Test
    fun `one degree along equator is approximately 111 kilometres`() {
        assertEquals(111.195, Location(0.0, 0.0).distanceTo(Location(0.0, 1.0)), 0.001)
    }

    @Test
    fun `distance is symmetric`() {
        val first = Location(51.59, 4.78)
        val second = Location(52.37, 4.90)
        assertEquals(first.distanceTo(second), second.distanceTo(first), 0.000001)
    }

    @Test
    fun `takes short path across international date line`() {
        assertEquals(222.390, Location(0.0, 179.0).distanceTo(Location(0.0, -179.0)), 0.001)
    }

    @Test
    fun `opposite points return finite half circumference`() {
        assertEquals(20015.087, Location(0.0, 0.0).distanceTo(Location(0.0, 180.0)), 0.001)
    }

    @Test
    fun `radius includes boundary and excludes points beyond it`() {
        val first = Location(0.0, 0.0)
        val second = Location(0.0, 1.0)
        val distance = first.distanceTo(second)
        assertTrue(first.isWithinRadius(second, distance))
        assertTrue(first.isWithinRadius(second, distance + 0.001))
        assertFalse(first.isWithinRadius(second, distance - 0.001))
        assertTrue(first.isWithinRadius(first, 0.0))
        assertFalse(first.isWithinRadius(second, 0.0))
    }

    @Test
    fun `rejects invalid radius`() {
        val location = Location(0.0, 0.0)
        for (radius in listOf(-0.001, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { location.isWithinRadius(location, radius) }
        }
    }
}
