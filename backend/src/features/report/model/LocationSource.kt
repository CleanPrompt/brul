package nl.brul.backend.features.report.model

/** Where the location of a photo came from. */
enum class LocationSource {
    /** From the EXIF data that the phone camera writes into the photo. */
    PHOTO,

    /** Sent by the app, because the photo itself had no GPS data. */
    APP
}
