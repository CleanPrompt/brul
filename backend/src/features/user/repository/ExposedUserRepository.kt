package nl.example.backend.features.user.repository

import nl.example.backend.features.user.database.UsersTable
import nl.example.backend.features.user.mapper.toUser
import nl.example.backend.features.user.model.User
import nl.example.backend.features.user.model.UserQuery
import nl.example.backend.features.user.model.UserSortField
import nl.example.backend.shared.database.containsIgnoringCase
import nl.example.backend.shared.database.toSortOrder
import nl.example.backend.shared.error.AppException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ExposedUserRepository : UserRepository {
    override fun findById(id: UUID): User? = transaction {
        UsersTable.selectAll().where { UsersTable.id eq id }
            .singleOrNull()?.toUser()
    }

    override fun findByEmail(email: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.email eq email }
            .singleOrNull()?.toUser()
    }

    override fun search(query: UserQuery): List<User> = transaction {
        filtered(query)
            // The id as a second sort key keeps the order stable, so no user appears on two pages.
            .orderBy(
                sortColumn(query.sortField) to query.sortDirection.toSortOrder(),
                UsersTable.id to SortOrder.ASC
            )
            .limit(query.page.limit)
            .offset(query.page.offset.toLong())
            .map { it.toUser() }
    }

    override fun count(query: UserQuery): Long = transaction {
        filtered(query).count()
    }

    /** Starts with every row and adds one `AND` condition for each filter that is set. */
    private fun filtered(query: UserQuery): Query {
        val rows = UsersTable.selectAll()
        query.active?.let { active -> rows.andWhere { UsersTable.active eq active } }
        query.text?.let { text ->
            rows.andWhere {
                UsersTable.email.containsIgnoringCase(text) or UsersTable.displayName.containsIgnoringCase(text)
            }
        }
        return rows
    }

    private fun sortColumn(field: UserSortField): Column<*> =
        when (field) {
            UserSortField.CREATED_AT -> UsersTable.createdAt
            UserSortField.DISPLAY_NAME -> UsersTable.displayName
            UserSortField.EMAIL -> UsersTable.email
        }

    /**
     * The unique index on `email` is the final guard: if two registrations with the same email
     * pass the service check at the same time, the database rejects the second insert.
     */
    override fun save(user: User): User =
        try {
            transaction {
                // Exposed retries failed transactions by default; a duplicate email will not succeed on retry.
                maxAttempts = 1
                UsersTable.insert {
                    it[id] = user.id
                    it[email] = user.email
                    it[displayName] = user.displayName
                    it[passwordHash] = user.passwordHash
                    it[active] = user.active
                    it[createdAt] = user.createdAt
                }
            }
            user
        } catch (exception: ExposedSQLException) {
            if (exception.sqlState == UNIQUE_VIOLATION) {
                throw AppException.Conflict("Email address is already registered", "EMAIL_ALREADY_REGISTERED")
            }
            throw exception
        }

    override fun update(user: User): Boolean = transaction {
        UsersTable.update({ UsersTable.id eq user.id }) {
            it[email] = user.email
            it[displayName] = user.displayName
            it[passwordHash] = user.passwordHash
            it[active] = user.active
        } > 0
    }

    /**
     * One atomic SQL statement (`token_version = token_version + 1`), so two logouts at the same time
     * cannot overwrite each other.
     */
    override fun incrementTokenVersion(id: UUID) {
        transaction {
            UsersTable.update({ UsersTable.id eq id }) {
                with(SqlExpressionBuilder) {
                    it[tokenVersion] = tokenVersion + 1
                }
            }
        }
    }

    private companion object {
        /** Standard SQL state for a unique constraint violation. */
        const val UNIQUE_VIOLATION = "23505"
    }
}
