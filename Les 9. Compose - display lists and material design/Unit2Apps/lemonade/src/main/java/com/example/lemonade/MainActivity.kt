package com.example.lemonade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lemonade.ui.theme.LemonadeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LemonadeTheme {
                LemonadeApp()
            }
        }
    }
}

/**
 * The four steps of making lemonade, and how many times the lemon still has to be squeezed in step 2.
 * Kept separate from the UI so [nextState] can be unit tested.
 */
@VisibleForTesting
internal data class LemonadeState(val step: Int = 1, val squeezesLeft: Int = 0)

/**
 * What happens when the image is tapped:
 * 1. tree -> pick a lemon, which needs 2 to 4 squeezes ([newSqueezes]),
 * 2. squeeze once more, or go to the glass after the last squeeze,
 * 3. drink -> empty glass,
 * 4. empty glass -> back to the tree.
 */
@VisibleForTesting
internal fun nextState(
    state: LemonadeState,
    newSqueezes: () -> Int = { (2..4).random() }
): LemonadeState = when (state.step) {
    1 -> LemonadeState(step = 2, squeezesLeft = newSqueezes())
    2 -> if (state.squeezesLeft <= 1) LemonadeState(step = 3) else state.copy(squeezesLeft = state.squeezesLeft - 1)
    3 -> LemonadeState(step = 4)
    else -> LemonadeState(step = 1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LemonadeApp() {
    // Two Ints instead of the data class, because rememberSaveable can store Ints without extra code.
    var step by rememberSaveable { mutableIntStateOf(1) }
    var squeezesLeft by rememberSaveable { mutableIntStateOf(0) }

    val onImageClick = {
        val next = nextState(LemonadeState(step, squeezesLeft))
        step = next.step
        squeezesLeft = next.squeezesLeft
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (step) {
                1 -> LemonTextAndImage(
                    textLabelResourceId = R.string.lemon_select,
                    drawableResourceId = R.drawable.lemon_tree,
                    contentDescriptionResourceId = R.string.lemon_tree_content_description,
                    onImageClick = onImageClick
                )
                2 -> LemonTextAndImage(
                    textLabelResourceId = R.string.lemon_squeeze,
                    drawableResourceId = R.drawable.lemon_squeeze,
                    contentDescriptionResourceId = R.string.lemon_content_description,
                    onImageClick = onImageClick
                )
                3 -> LemonTextAndImage(
                    textLabelResourceId = R.string.lemon_drink,
                    drawableResourceId = R.drawable.lemon_drink,
                    contentDescriptionResourceId = R.string.lemonade_content_description,
                    onImageClick = onImageClick
                )
                else -> LemonTextAndImage(
                    textLabelResourceId = R.string.lemon_empty_glass,
                    drawableResourceId = R.drawable.lemon_restart,
                    contentDescriptionResourceId = R.string.empty_glass_content_description,
                    onImageClick = onImageClick
                )
            }
        }
    }
}

/** One step: a large tappable image with the instruction underneath. Stateless and reused for all four steps. */
@Composable
fun LemonTextAndImage(
    @StringRes textLabelResourceId: Int,
    @DrawableRes drawableResourceId: Int,
    @StringRes contentDescriptionResourceId: Int,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onImageClick,
            shape = RoundedCornerShape(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Image(
                painter = painterResource(drawableResourceId),
                contentDescription = stringResource(contentDescriptionResourceId),
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(textLabelResourceId),
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp
        )
    }
}

@Preview(showBackground = true, name = "English")
@Preview(showBackground = true, name = "Nederlands", locale = "nl")
@Composable
fun LemonadePreview() {
    LemonadeTheme {
        LemonadeApp()
    }
}
