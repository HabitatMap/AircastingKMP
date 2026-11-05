package pl.llp.aircasting.explore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.domain.InnerBound
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.chart.ElevationLight1
import pl.llp.aircasting.ui.theme.AqBandColors
import kotlin.math.roundToInt

private val ThumbSize = 24.dp
private val TrackHeight = 10.dp
private val TrackTop = 7.dp // centres the 10 dp track on the 24 dp thumbs
private val LabelTop = 27.dp // Figma: labels 20 dp below the track's top
private val SliderHeight = 43.dp // LabelTop + one 16 dp label line

/**
 * Figma "Threshold control" (1900:525): three draggable inner bounds over the band colours.
 * The ends are fixed; they're edited in the Thresholds sheet.
 */
@Composable
fun ThresholdSlider(
  threshold: SensorThreshold,
  unit: String,
  onChange: (SensorThreshold) -> Unit,
  modifier: Modifier = Modifier,
) {
  // The drag edits this local copy; onChange (DB write → whole screen recolours) fires once, on
  // release, like legacy HLUSlider.onStopTrackingTouch. Keyed on threshold: a save or a stream
  // switch replaces it.
  var draft by remember(threshold) { mutableStateOf(threshold) }
  // pointerInput below restarts only when threshold changes; this keeps it calling the latest onChange.
  val save by rememberUpdatedState(onChange)
  val span = (threshold.veryHigh - threshold.veryLow).coerceAtLeast(1).toFloat() // never ÷0
  val labelStyle = MaterialTheme.typography.bodySmall
  val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
  Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(
      "${LocalStrings.current.colourThresholds}  ·  $unit",
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
    BoxWithConstraints(
      Modifier.fillMaxWidth().height(SliderHeight).pointerInput(threshold) {
        fun valueAt(x: Float) = threshold.veryLow + (x / size.width * span).roundToInt()
        var bound = InnerBound.Low
        // Horizontal only: a vertical swipe starting here still scrolls the screen.
        detectHorizontalDragGestures(
          onDragStart = { bound = draft.nearestBound(valueAt(it.x)) },
          onDragEnd = { if (draft != threshold) save(draft) },
          onDragCancel = { draft = threshold },
        ) { change, _ ->
          change.consume()
          draft = draft.moveBound(bound, valueAt(change.position.x))
        }
      },
    ) {
      val width = constraints.maxWidth
      fun xOf(value: Int) = (value - threshold.veryLow) / span * width

      Canvas(Modifier.padding(top = TrackTop).fillMaxWidth().height(TrackHeight).clip(RoundedCornerShape(8.dp))) {
        draft.bands.forEach { (range, level) ->
          drawRect(
            AqBandColors.forLevel(level),
            topLeft = Offset(xOf(range.first), 0f),
            size = Size(xOf(range.last) - xOf(range.first), size.height),
          )
        }
      }
      Text("${threshold.veryLow}", Modifier.align(Alignment.TopStart).padding(top = LabelTop), style = labelStyle, color = labelColor)
      Text("${threshold.veryHigh}", Modifier.align(Alignment.TopEnd).padding(top = LabelTop), style = labelStyle, color = labelColor)
      InnerBound.entries.forEach { bound ->
        val value = draft[bound]
        Box(
          Modifier
            .offset { IntOffset((xOf(value) - ThumbSize.toPx() / 2).roundToInt(), 0) }
            .size(ThumbSize)
            .dropShadow(CircleShape, ElevationLight1[0])
            .dropShadow(CircleShape, ElevationLight1[1])
            .background(Color.White, CircleShape),
        )
        Text("$value", Modifier.padding(top = LabelTop).centredOn { xOf(value) }, style = labelStyle, color = labelColor)
      }
    }
  }
}

/** Places the child with its horizontal centre on [x] px, instead of its left edge. */
private fun Modifier.centredOn(x: () -> Float) = layout { measurable, constraints ->
  val placeable = measurable.measure(constraints.copy(minWidth = 0))
  layout(placeable.width, placeable.height) { placeable.place((x() - placeable.width / 2f).roundToInt(), 0) }
}
