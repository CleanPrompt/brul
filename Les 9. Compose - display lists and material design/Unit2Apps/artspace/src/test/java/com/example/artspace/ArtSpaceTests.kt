package com.example.artspace

import org.junit.Assert.assertEquals
import org.junit.Test

/** Local unit tests for the gallery navigation; they run without an emulator. */
class ArtSpaceTests {

    @Test
    fun next_movesOneForward() {
        assertEquals(1, nextIndex(current = 0, size = 4))
    }

    @Test
    fun next_afterLastWrapsToFirst() {
        assertEquals(0, nextIndex(current = 3, size = 4))
    }

    @Test
    fun previous_beforeFirstWrapsToLast() {
        assertEquals(3, previousIndex(current = 0, size = 4))
    }

    @Test
    fun nextThenPrevious_returnsToStart() {
        assertEquals(2, previousIndex(nextIndex(current = 2, size = 4), size = 4))
    }
}
