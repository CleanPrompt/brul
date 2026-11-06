package nl.brul.backend.shared.paging

import kotlinx.serialization.Serializable

/**
 * One page of a list, for any item type [T] (e.g. `PageResponse<UserResponse>`).
 *
 * [total] counts every item that matches the filters, not only this page, so a client can tell
 * whether there is a next page: `offset + items.size < total`.
 */
@Serializable
data class PageResponse<T>(
    val items: List<T>,
    val total: Long,
    val limit: Int,
    val offset: Int
)
