package nl.example.backend.shared.http

import io.ktor.http.parametersOf
import nl.example.backend.shared.error.AppException
import nl.example.backend.shared.paging.Page
import nl.example.backend.shared.paging.SortDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class QueryParametersTest {
    private val none = parametersOf()

    @Test
    fun `missing parameters give null or the defaults`() {
        assertNull(none.searchText("q"))
        assertNull(none.booleanOrNull("active"))
        assertNull(none.uuidOrNull("userId"))
        assertEquals(SortDirection.DESC, none.sortDirection())
        assertEquals(Page(limit = Page.DEFAULT_LIMIT, offset = 0), none.page())
    }

    @Test
    fun `search text is trimmed and blank means no filter`() {
        assertEquals("alice", parametersOf("q", "  alice  ").searchText("q"))
        assertNull(parametersOf("q", "   ").searchText("q"))
    }

    @Test
    fun `reads valid values`() {
        assertEquals(true, parametersOf("active", "true").booleanOrNull("active"))
        assertEquals(SortDirection.ASC, parametersOf("order", "asc").sortDirection())
        val paging = parametersOf("limit" to listOf("5"), "offset" to listOf("10"))
        assertEquals(Page(limit = 5, offset = 10), paging.page())
        assertEquals(2, parametersOf("size", "two").oneOf("size", mapOf("one" to 1, "two" to 2)))
    }

    @Test
    fun `invalid values give 400`() {
        val tooLong = "x".repeat(MAX_SEARCH_LENGTH + 1)
        assertFailsWith<AppException.BadRequest> { parametersOf("q", tooLong).searchText("q") }
        assertFailsWith<AppException.BadRequest> { parametersOf("active", "yes").booleanOrNull("active") }
        assertFailsWith<AppException.BadRequest> { parametersOf("userId", "nope").uuidOrNull("userId") }
        assertFailsWith<AppException.BadRequest> { parametersOf("order", "up").sortDirection() }
        assertFailsWith<AppException.BadRequest> { parametersOf("limit", "0").page() }
        assertFailsWith<AppException.BadRequest> { parametersOf("limit", "101").page() }
        assertFailsWith<AppException.BadRequest> { parametersOf("limit", "ten").page() }
        assertFailsWith<AppException.BadRequest> { parametersOf("offset", "-1").page() }
    }
}
