package nl.brul.backend.features.user.model

import nl.brul.backend.shared.paging.Page
import nl.brul.backend.shared.paging.SortDirection

/**
 * What `GET /users` asks for: filters, sorting and a page. A filter that is `null` is not applied.
 * The defaults give the newest accounts first.
 */
data class UserQuery(
    /** Matches part of the email or the display name, ignoring case. */
    val text: String? = null,
    /** `true` for active accounts, `false` for deactivated ones. */
    val active: Boolean? = null,
    val sortField: UserSortField = UserSortField.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val page: Page = Page()
)
