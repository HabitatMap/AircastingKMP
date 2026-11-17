package pl.llp.aircasting.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.TrackPoint
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.chart.hourMinuteSecondUtc
import pl.llp.aircasting.ui.chart.wallClockPoint
import kotlin.math.roundToInt
import kotlin.time.Instant

/** Same wall-clock x the graph builds, through the same formatter: map and graph tooltips agree. */
internal fun tooltipTime(time: Instant, timeZone: TimeZone): String =
  hourMinuteSecondUtc(wallClockPoint(time, value = 0.0, timeZone).x)

@Composable
internal fun MapTooltip(
  point: TrackPoint, unit: String, timeZone: TimeZone, levelColour: Color?, onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val muted = MaterialTheme.colorScheme.onSurfaceVariant
  Surface(
    modifier,
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.background,
    shadowElevation = 1.dp
  ) {
    // clickable inside the Surface: outside, its clip would cut off the shadow.
    Column(
      Modifier.clickable(onClickLabel = strings.mapTooltipClose, onClick = onClose)
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(point.time?.let { tooltipTime(it, timeZone) }
          .orEmpty(), style = MaterialTheme.typography.labelSmall, color = muted)
        Text("×", style = MaterialTheme.typography.labelMedium, color = muted)
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        levelColour?.let {
          Box(
            Modifier.size(6.dp)
              .background(it, CircleShape)
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            point.value?.roundToInt()
              ?.toString() ?: "–",
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            unit,
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.labelSmall,
            color = muted
          )
        }
      }
    }
  }
}

/**
 * Pins the tooltip beside dot [index], vertically centred on it (Figma: 18 dp right of the dot's
 * centre); flips to the left near the right edge. [camera] is read in the placement phase, so
 * panning re-places the bubble without recomposing it — the dots' draw-phase trick, one phase earlier.
 */
internal fun Modifier.anchoredTo(dots: TrackDots, index: Int, camera: () -> MapCamera): Modifier =
  layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    // The overlay Box hands down its own size as the max: that's the map's size.
    val mapWidth = constraints.maxWidth
    val mapHeight = constraints.maxHeight
    layout(placeable.width, placeable.height) {
      val screen = ScreenTransform(
        camera(),
        mapWidth.toFloat(),
        mapHeight.toFloat(),
        tileSizePx = 256f * density
      )
      val x = screen.screenX(dots.worldX[index])
      val y = screen.screenY(dots.worldY[index])
      val offset = TooltipOffset.toPx()
      val left =
        if (x + offset + placeable.width <= mapWidth) x + offset else x - offset - placeable.width
      placeable.place(left.roundToInt(), (y - placeable.height / 2f).roundToInt())
    }
  }

private val TooltipOffset = 18.dp