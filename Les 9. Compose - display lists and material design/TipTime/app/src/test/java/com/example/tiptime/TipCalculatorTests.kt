package com.example.tiptime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.NumberFormat

/**
 * Local unit tests for [calculateTip]. They run on the computer, without an emulator.
 * The expected values are formatted the same way as the app does, so the tests pass in every locale.
 */
class TipCalculatorTests {

    private fun money(value: Double): String = NumberFormat.getCurrencyInstance().format(value)

    @Test
    fun calculateTip_20PercentNoRoundUp() {
        val actualTip = calculateTip(amount = 10.00, tipPercent = 20.00, roundUp = false)
        assertEquals(money(2.0), actualTip)
    }

    @Test
    fun calculateTip_15PercentRoundUp() {
        // 15% of 10.00 is 1.50; rounding up gives 2.00.
        val actualTip = calculateTip(amount = 10.00, tipPercent = 15.00, roundUp = true)
        assertEquals(money(2.0), actualTip)
    }

    @Test
    fun calculateTip_zeroAmountGivesZeroTip() {
        val actualTip = calculateTip(amount = 0.0, tipPercent = 20.00, roundUp = true)
        assertEquals(money(0.0), actualTip)
    }

    @Test
    fun calculateTip_defaultPercentageIs15() {
        val actualTip = calculateTip(amount = 100.00, roundUp = false)
        assertEquals(money(15.0), actualTip)
    }
}
