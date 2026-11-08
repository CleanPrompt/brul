package nl.brul.backend.features.report.model

import nl.brul.backend.shared.error.requireValid
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on earth in degrees, e.g. `Location(51.5866, 4.7760)` for Breda. */
data class Location(
    val latitude: Double,
    val longitude: Double
) {
    init {
        requireValid(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        requireValid(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }

    /** The distance in kilometres along the surface of the earth (the haversine formula). */
    fun distanceKmTo(other: Location): Double {
        val latitudeDifference = Math.toRadians(other.latitude - latitude)
        val longitudeDifference = Math.toRadians(other.longitude - longitude)

        val a = sin(latitudeDifference / 2).pow(2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(longitudeDifference / 2).pow(2)

        return EARTH_RADIUS_KM * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    private companion object {
        const val EARTH_RADIUS_KM = 6371.0
    }
}
