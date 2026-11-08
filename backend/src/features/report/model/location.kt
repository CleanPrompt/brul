
package nl.brul.backend.features.report.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents a geographical location using GPS coordinates.
 *
 * Latitude: -90 to 90 degrees.
 * Longitude: -180 to 180 degrees.
 */
data class Location(
    val latitude: Double,
    val longitude: Double
) {

    // Validate GPS coordinates when creating a Location.
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Latitude must be between -90 and 90"
        }

        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitude must be between -180 and 180"
        }
    }

    /**
     * Calculates the distance to another location
     * using the Haversine formula.
     *
     * @return Distance in kilometers.
     */
    fun distanceTo(other: Location): Double {

        val earthRadiusKm = 6371.0

        // Convert latitude and longitude differences to radians.
        val latDifference =
            Math.toRadians(other.latitude - latitude)

        val lonDifference =
            Math.toRadians(other.longitude - longitude)

        // Haversine calculation.
        val a =
            sin(latDifference / 2).let { it * it } +
                    cos(Math.toRadians(latitude)) *
                    cos(Math.toRadians(other.latitude)) *
                    sin(lonDifference / 2).let { it * it }

        val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))

        // Return distance in kilometers.
        return earthRadiusKm * c
    }

    /**
     * Checks whether another location is within
     * the specified radius in kilometers.
     */
    fun isWithinRadius(
        other: Location,
        radiusKm: Double
    ): Boolean {

        require(radiusKm.isFinite() && radiusKm >= 0.0) {
            "Radius must be a non-negative finite number"
        }

        return distanceTo(other) <= radiusKm
    }
}
