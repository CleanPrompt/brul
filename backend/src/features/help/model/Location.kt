package nl.brul.backend.features.help.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Value Object representing geographic coordinates.
 * Validates coordinate bounds and provides distance calculation using the Haversine formula.
 */
data class Location(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) {
            "Latitude must be between -90 and 90."
        }

        require(longitude in -180.0..180.0) {
            "Longitude must be between -180 and 180."
        }
    }

    /**
     * Calculates the great-circle distance between two points on Earth in kilometers.
     */
    fun distanceTo(other: Location): Double {
        val earthRadiusKm = 6371.0

        val latitudeDifference = Math.toRadians(other.latitude - latitude)
        val longitudeDifference = Math.toRadians(other.longitude - longitude)

        val firstLatitude = Math.toRadians(latitude)
        val secondLatitude = Math.toRadians(other.latitude)

        val a = sin(latitudeDifference / 2).pow(2) +
            cos(firstLatitude) *
            cos(secondLatitude) *
            sin(longitudeDifference / 2).pow(2)

        val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))

        return earthRadiusKm * c
    }
}
