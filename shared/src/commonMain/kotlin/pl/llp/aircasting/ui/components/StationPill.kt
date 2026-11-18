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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

/**
 * A station on a map: halo + white pill with a coloured marker and the label; blue
 * (primaryContainer) when uncoloured. The marker tells the source apart: the AirBeam glyph
 * (Figma 1138:28376) for an [airBeam] station, a dot for a government one. Explore's Android
 * markers and the details map both draw it; ExploreMap.ios.kt has a UIKit twin with the same metrics.
 */
@Composable
fun StationPill(label: String, levelColour: Color?, airBeam: Boolean, modifier: Modifier = Modifier) {
  val colour = levelColour ?: MaterialTheme.colorScheme.primaryContainer
  Box(modifier, contentAlignment = Alignment.Center) {
    Box(Modifier.size(56.dp).background(colour.copy(alpha = 0.4f), CircleShape))
    Surface(shape = RoundedCornerShape(50), shadowElevation = 2.dp, color = Color.White) {
      Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (airBeam) Icon(
          painterResource(Res.drawable.ic_airbeam_marker),
          contentDescription = null, // the label carries the meaning
          tint = colour,
        )
        else Box(Modifier.size(12.dp).background(colour, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Black)
      }
    }
  }
}
