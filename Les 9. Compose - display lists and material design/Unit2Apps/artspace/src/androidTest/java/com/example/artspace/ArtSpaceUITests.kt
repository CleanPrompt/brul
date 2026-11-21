package com.example.artspace

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.artspace.ui.theme.ArtSpaceTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI tests: they run on an emulator or a phone.
 * Texts come from the string resources, so the tests also pass when the device is set to Dutch.
 */
class ArtSpaceUITests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun showGallery() {
        composeTestRule.setContent {
            ArtSpaceTheme {
                ArtSpaceApp()
            }
        }
    }

    @Test
    fun startsWithFirstArtwork() {
        composeTestRule.onNodeWithText(context.getString(R.string.title_sunset)).assertExists()
    }

    @Test
    fun next_showsSecondArtwork() {
        composeTestRule.onNodeWithText(context.getString(R.string.next)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.title_circles)).assertExists()
    }

    @Test
    fun previous_onFirstShowsLastArtwork() {
        composeTestRule.onNodeWithText(context.getString(R.string.previous)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.title_blocks)).assertExists()
    }
}
