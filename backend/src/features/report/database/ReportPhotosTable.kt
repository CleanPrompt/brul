package nl.brul.backend.features.report.database

import nl.brul.backend.features.report.model.LocationSource
import nl.brul.backend.features.user.database.UsersTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/** The data of every photo. The image itself is stored in `PhotoStorage`, under `storage_key`. */
object ReportPhotosTable : Table("report_photos") {
    val id = uuid("id")
    /** CASCADE: when a report is deleted, its photo rows go too. `ReportService` removes the files. */
    val reportId = reference("report_id", ReportsTable.id, onDelete = ReferenceOption.CASCADE)
    val uploaderId = reference("uploader_id", UsersTable.id)
    val contentType = varchar("content_type", 30)
    val sizeBytes = long("size_bytes")
    val storageKey = varchar("storage_key", 100).uniqueIndex()
    val createdAt = timestamp("created_at")
    /** All three are empty when the photo has no location. The source is stored as the enum name (`PHOTO`). */
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val locationSource = enumerationByName("location_source", 10, LocationSource::class).nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, reportId)
    }
}
