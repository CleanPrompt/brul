package nl.brul.backend.config

import nl.brul.backend.features.authorization.database.PermissionsTable
import nl.brul.backend.features.authorization.database.RolePermissionsTable
import nl.brul.backend.features.authorization.database.RolesTable
import nl.brul.backend.features.authorization.database.UserRolesTable
import nl.brul.backend.features.feedback.database.FeedbackTable
import nl.brul.backend.features.user.database.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(config: AppConfig) {
        Database.connect(
            url = config.databaseUrl,
            driver = config.databaseDriver,
            user = config.databaseUser,
            password = config.databasePassword
        )

        transaction {
            SchemaUtils.create(
                UsersTable,
                RolesTable,
                PermissionsTable,
                UserRolesTable,
                RolePermissionsTable,
                FeedbackTable
            )
        }
    }
}
