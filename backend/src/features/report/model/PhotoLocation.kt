package nl.brul.backend.features.report.model

/**
 * Where a photo was taken and where that position came from.
 * Keeping both in one class means a location can never exist without its source.
 */
data class PhotoLocation(
    val location: Location,
    val source: LocationSource
)
