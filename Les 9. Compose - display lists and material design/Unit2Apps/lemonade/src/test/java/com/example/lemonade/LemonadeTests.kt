package com.example.lemonade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Local unit tests for the step logic in [nextState]; they run without an emulator. */
class LemonadeTests {

    @Test
    fun tree_goesToSqueezeWithGivenSqueezes() {
        val next = nextState(LemonadeState(step = 1), newSqueezes = { 3 })
        assertEquals(LemonadeState(step = 2, squeezesLeft = 3), next)
    }

    @Test
    fun squeeze_needsExactlyTheGivenNumberOfTaps() {
        var state = nextState(LemonadeState(step = 1), newSqueezes = { 3 })
        repeat(2) {
            state = nextState(state)
            assertEquals(2, state.step)
        }
        state = nextState(state)
        assertEquals(3, state.step)
    }

    @Test
    fun drinkAndEmptyGlass_goBackToTree() {
        val empty = nextState(LemonadeState(step = 3))
        assertEquals(4, empty.step)
        assertEquals(LemonadeState(step = 1), nextState(empty))
    }

    @Test
    fun defaultSqueezes_areBetweenTwoAndFour() {
        repeat(200) {
            val squeezes = nextState(LemonadeState(step = 1)).squeezesLeft
            assertTrue("Got $squeezes", squeezes in 2..4)
        }
    }
}
