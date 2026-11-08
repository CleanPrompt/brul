package nl.brul.backend.features.report.repository

import nl.brul.backend.features.report.database.ReportPhotosTable
import nl.brul.backend.features.report.mapper.toReportPhoto
import nl.brul.backend.features.report.model.ReportPhoto
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ExposedReportPhotoRepository : ReportPhotoRepository {
    override fun findById(id: UUID): ReportPhoto? = transaction {
        ReportPhotosTable.selectAll().where { ReportPhotosTable.id eq id }
            .singleOrNull()?.toReportPhoto()
    }

    override fun findByReport(reportId: UUID): List<ReportPhoto> = transaction {
        ReportPhotosTable.selectAll().where { ReportPhotosTable.reportId eq reportId }
            .orderBy(ReportPhotosTable.createdAt to SortOrder.ASC, ReportPhotosTable.id to SortOrder.ASC)
            .map { it.toReportPhoto() }
    }

    override fun countByReport(reportId: UUID): Long = transaction {
        ReportPhotosTable.selectAll().where { ReportPhotosTable.reportId eq reportId }.count()
    }

    override fun save(photo: ReportPhoto): ReportPhoto {
        transaction {
            ReportPhotosTable.insert {
                it[id] = photo.id
                it[reportId] = photo.reportId
                it[uploaderId] = photo.uploaderId
                it[contentType] = photo.contentType
                it[sizeBytes] = photo.sizeBytes
                it[storageKey] = photo.storageKey
                it[createdAt] = photo.createdAt
                it[latitude] = photo.location?.location?.latitude
                it[longitude] = photo.location?.location?.longitude
                it[locationSource] = photo.location?.source
            }
        }
        return photo
    }

    override fun delete(id: UUID): Boolean = transaction {
        ReportPhotosTable.deleteWhere { ReportPhotosTable.id eq id } > 0
    }
}
