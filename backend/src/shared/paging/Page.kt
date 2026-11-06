package nl.brul.backend.shared.paging

/**
 * Which part of a result list to return: at most [limit] items, after skipping the first [offset] items.
 * A model, not a DTO: it is read from the query parameters `limit` and `offset` by `Parameters.page()`.
 */
data class Page(
    val limit: Int = DEFAULT_LIMIT,
    val offset: Int = 0
) {
    companion object {
        const val DEFAULT_LIMIT = 20
        /** Upper bound for `limit`, so one request can never return the whole table. */
        const val MAX_LIMIT = 100
    }
}
