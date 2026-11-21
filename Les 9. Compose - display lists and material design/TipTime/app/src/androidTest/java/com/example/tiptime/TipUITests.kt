package com.example.tiptime

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.tiptime.ui.theme.TipTimeTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.text.NumberFormat

/**
 * Instrumented UI tests: they run on an emulator or a phone (right-click this file > Run).
 * The texts come from the string resources, so the tests also pass when the device is set to Dutch.
 */
class TipUITests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun money(value: Double): String = NumberFormat.getCurrencyInstance().format(value)

    @Before
    fun showScreen() {
        composeTestRule.setContent {
            TipTimeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TipTimeLayout()
                }
            }
        }
    }

    @Test
    fun calculate_20_percent_tip() {
        composeTestRule.onNodeWithText(context.getString(R.string.bill_amount)).performTextInput("10")
        composeTestRule.onNodeWithText(context.getString(R.string.how_was_the_service)).performTextInput("20")

        val expected = context.getString(R.string.tip_amount, money(2.0))
        composeTestRule.onNodeWithText(expected).assertExists("No node with this text was found.")
    }

    @Test
    fun round_up_switch_rounds_the_tip() {
        composeTestRule.onNodeWithText(context.getString(R.string.bill_amount)).performTextInput("10")
        composeTestRule.onNodeWithText(context.getString(R.string.how_was_the_service)).performTextInput("15")

        val roundUpSwitch = composeTestRule.onNode(isToggleable())
        roundUpSwitch.assertIsOff()
        roundUpSwitch.performClick()
        roundUpSwitch.assertIsOn()

        val expected = context.getString(R.string.tip_amount, money(2.0))
        composeTestRule.onNodeWithText(expected).assertExists("The tip was not rounded up to 2.00.")
    }

    @Test
    fun empty_fields_show_zero_tip() {
        val expected = context.getString(R.string.tip_amount, money(0.0))
        composeTestRule.onNodeWithText(expected).assertExists("The start screen should show a tip of 0.")
    }
}
