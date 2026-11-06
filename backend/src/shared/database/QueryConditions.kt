package nl.example.backend.shared.database

import nl.example.backend.shared.paging.SortDirection
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.LikePattern
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.lowerCase

/**
 * `LOWER(column) LIKE '%text%'`: the column contains [text], ignoring upper and lower case.
 * `%` and `_` in [text] are matched literally, so a user cannot inject their own wildcards.
 * Call it inside a `transaction`, because escaping depends on the database dialect.
 */
fun Expression<String>.containsIgnoringCase(text: String): Op<Boolean> {
    val literal = LikePattern.ofLiteral(text.lowercase())
    return lowerCase() like LikePattern("%${literal.pattern}%", literal.escapeChar)
}

/** Our own [SortDirection] keeps services free of Exposed; repositories convert it here. */
fun SortDirection.toSortOrder(): SortOrder =
    when (this) {
        SortDirection.ASC -> SortOrder.ASC
        SortDirection.DESC -> SortOrder.DESC
    }
