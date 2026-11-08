package nl.brul.backend.features.help.mapper

import nl.brul.backend.features.help.database.HelpParticipationsTable
import nl.brul.backend.features.help.model.HelpParticipation
import org.jetbrains.exposed.sql.ResultRow

/**
 * Extension function converting an Exposed ResultRow from the database into the domain model.
 */
fun ResultRow.toHelpParticipation(): HelpParticipation =
    HelpParticipation(
        id = this[HelpParticipationsTable.id],
        helpRequestId = this[HelpParticipationsTable.helpRequestId],
        userId = this[HelpParticipationsTable.userId],
        status = this[HelpParticipationsTable.status],
        checkedInAt = this[HelpParticipationsTable.checkedInAt]
    )
