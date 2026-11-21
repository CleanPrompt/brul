package com.example.diceroller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Local unit tests for [rollDice]; they run without an emulator. */
class DiceRollerTests {

    @Test
    fun rollDice_alwaysBetweenOneAndSix() {
        val random = Random(seed = 42)
        repeat(1_000) {
            val result = rollDice(random)
            assertTrue("Got $result", result in 1..6)
        }
    }

    @Test
    fun rollDice_canRollEveryFace() {
        val random = Random(seed = 7)
        val seen = List(1_000) { rollDice(random) }.toSet()
        assertEquals(setOf(1, 2, 3, 4, 5, 6), seen)
    }

    @Test
    fun rollDice_sameSeedGivesSameRolls() {
        val first = List(10) { rollDice(Random(seed = 1)) }
        val second = List(10) { rollDice(Random(seed = 1)) }
        assertEquals(first, second)
    }
}
