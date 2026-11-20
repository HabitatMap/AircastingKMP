package pl.llp.aircasting.ui.components

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_airbeam_marker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

/**
 * A station on a map: halo + white pill with a coloured marker and the label; blue
 * (primaryContainer) when uncoloured. The marker tells the source apart: the AirBeam glyph
 * (Figma 1138:28376) for an [airBeam] station, a dot for a government one. Explore's Android
 * markers and the details map both draw it; ExploreMap.ios.kt has a UIKit twin with the same metrics.
 *
 * The halo is centred on the marker, not the pill (Figma 783:5396): the pill hangs off to the right.
 * The layout is padded to be symmetric around the marker, so callers that centre this composable on
 * the station's coordinate (map anchor 0.5/0.5, `centredOn`) put the marker exactly on it.
 */
@Composable
fun StationPill(label: String, levelColour: Color?, airBeam: Boolean, modifier: Modifier = Modifier) {
  val colour = levelColour ?: MaterialTheme.colorScheme.primaryContainer
  val markerCentreX = PadStart + (if (airBeam) GlyphWidth else DotSize) / 2
  Layout(
    modifier = modifier,
    content = {
      Box(Modifier.size(HaloSize).background(colour.copy(alpha = 0.4f), CircleShape))
      // Figma "Tag" (783:5110): white in both themes — it sits on the map, not on a surface.
      Surface(shape = RoundedCornerShape(50), shadowElevation = 1.dp, color = Color.White) {
        Row(
          modifier = Modifier.padding(start = PadStart, end = PadEnd, top = PadY, bottom = PadY),
          horizontalArrangement = Arrangement.spacedBy(Gap),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (airBeam) Icon(
            painterResource(Res.drawable.ic_airbeam_marker),
            contentDescription = null, // the label carries the meaning
            modifier = Modifier.size(GlyphWidth, GlyphHeight),
            tint = colour,
          )
          else Box(Modifier.size(DotSize).background(colour, CircleShape))
          val type = MaterialTheme.typography.labelSmall
          Text(
            label,
            style = type.copy(
              fontSize = type.fontSize * Scale,
              lineHeight = type.lineHeight * Scale,
              letterSpacing = type.letterSpacing * Scale,
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
    },
  ) { (haloMeasurable, pillMeasurable), constraints ->
    val loose = constraints.copy(minWidth = 0, minHeight = 0)
    val halo = haloMeasurable.measure(loose)
    val pill = pillMeasurable.measure(loose)
    val markerX = markerCentreX.roundToPx()
    // Half-width around the marker: whichever reaches further — the halo, or the pill's right end.
    val half = maxOf(halo.width / 2, markerX, pill.width - markerX)
    val height = maxOf(halo.height, pill.height)
    layout(half * 2, height) {
      halo.place(half - halo.width / 2, (height - halo.height) / 2)
      pill.place(half - markerX, (height - pill.height) / 2)
    }
  }
}

// Every metric (text included) is the Figma size times Scale, so the pill shrinks without changing
// its proportions. ExploreMap.ios.kt uses the same SCALE.
private const val Scale = 0.8f
private val HaloSize = 56.dp * Scale
private val PadStart = 12.dp * Scale
private val PadEnd = 10.dp * Scale
private val PadY = 6.dp * Scale
private val Gap = 4.dp * Scale
private val DotSize = 12.dp * Scale
private val GlyphWidth = 14.dp * Scale // ic_airbeam_marker
private val GlyphHeight = 16.dp * Scale
