package pl.llp.aircasting.ui.chart

import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.common.Point
import kotlin.math.abs

/**
 * Vico's show-on-press, plus one rule: once the finger has travelled past the slop the chart pans
 * at, the tooltip hides until the finger lifts. Within it the tooltip follows the finger (scrub).
 *
 * The same slop must be the chart's touch slop (see MeasurementChart), so "scrubbing" and
 * "panning" switch at the same distance. Speed plays no part: Compose's drag starts on distance.
 *
 * @param panSlop sideways distance in px, where the chart starts to pan.
 * @param verticalSlop up/down distance in px, where the page around the chart starts to scroll.
 */
internal class ScrubUntilPanController(
  private val panSlop: Float,
  private val verticalSlop: Float,
) : CartesianMarkerController {
  private var down: Point? = null
  private var panning = false

  override val acceptsLongPress = false

  // The tooltip stays under the finger, as Vico's show-on-press.
  override val lock = CartesianMarkerController.Lock.Position

  override fun shouldAcceptInteraction(
    interaction: Interaction,
    targets: List<CartesianMarker.Target>
  ) =
    when (interaction) {
      is Interaction.Press -> true
      is Interaction.Move, is Interaction.Release -> down != null
      else -> false
    }

  override fun shouldShowMarker(
    interaction: Interaction,
    targets: List<CartesianMarker.Target>
  ): Boolean {
    when (interaction) {
      is Interaction.Press -> {
        down = interaction.point
        panning = false
      }

      is Interaction.Release -> {
        down = null
        return false
      }

      else -> {
        val start = down ?: return false
        if (abs(interaction.point.x - start.x) > panSlop || abs(interaction.point.y - start.y) > verticalSlop) {
          panning = true
        }
      }
    }
    return !panning
  }
}