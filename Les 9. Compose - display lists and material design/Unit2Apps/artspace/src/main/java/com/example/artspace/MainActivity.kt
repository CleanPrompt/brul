package com.example.artspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.artspace.ui.theme.ArtSpaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ArtSpaceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ArtSpaceApp()
                }
            }
        }
    }
}

/** One artwork in the gallery. */
data class Artwork(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val artist: Int,
    val year: Int
)

/** The gallery, in display order. Replace these with your own photos or drawings. */
val artworks = listOf(
    Artwork(R.drawable.art_sunset, R.string.title_sunset, R.string.artist_unknown, 2026),
    Artwork(R.drawable.art_circles, R.string.title_circles, R.string.artist_unknown, 2026),
    Artwork(R.drawable.art_windmill, R.string.title_windmill, R.string.artist_unknown, 2026),
    Artwork(R.drawable.art_blocks, R.string.title_blocks, R.string.artist_unknown, 2026)
)

/** Index of the next artwork; after the last one it starts again at the first. */
@VisibleForTesting
internal fun nextIndex(current: Int, size: Int): Int = (current + 1) % size

/** Index of the previous artwork; before the first one it wraps around to the last. */
@VisibleForTesting
internal fun previousIndex(current: Int, size: Int): Int = (current - 1 + size) % size

@Composable
fun ArtSpaceApp(modifier: Modifier = Modifier) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val artwork = artworks[index]

    Column(
        modifier = modifier
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ArtworkWall(image = artwork.image, title = artwork.title)
        Spacer(modifier = Modifier.height(24.dp))
        ArtworkDescriptor(title = artwork.title, artist = artwork.artist, year = artwork.year)
        Spacer(modifier = Modifier.height(24.dp))
        DisplayController(
            onPrevious = { index = previousIndex(index, artworks.size) },
            onNext = { index = nextIndex(index, artworks.size) }
        )
    }
}

/** The artwork in a raised frame, like a painting on a wall. */
@Composable
fun ArtworkWall(@DrawableRes image: Int, @StringRes title: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = stringResource(title),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(32.dp)
                .aspectRatio(1f)
        )
    }
}

/** Title, artist and year below the artwork. */
@Composable
fun ArtworkDescriptor(@StringRes title: Int, @StringRes artist: Int, year: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = stringResource(title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.artist_and_year, stringResource(artist), year),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** The Previous and Next buttons. */
@Composable
fun DisplayController(onPrevious: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(onClick = onPrevious, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.previous))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = onNext, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.next))
        }
    }
}

@Preview(showBackground = true, name = "English")
@Preview(showBackground = true, name = "Nederlands", locale = "nl")
@Composable
fun ArtSpacePreview() {
    ArtSpaceTheme {
        ArtSpaceApp()
    }
}
