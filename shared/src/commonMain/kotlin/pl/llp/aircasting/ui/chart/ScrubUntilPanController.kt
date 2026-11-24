package pl.llp.aircasting.ui.chart

import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.Interaction
import com.patrykandpatrick.vico.compose.common.Point
import kotlin.math.abs

/**
 * When the tooltip shows, which depends on whether the chart can pan (read once per press: one
 * finger can't change the zoom mid-gesture).
 *
 * **Zoomed in** (the chart can pan): a drag pans, it never scrubs through measurements. A press
 * alone shows nothing; **press and hold** shows the measurement under the finger, and pulling
 * sideways from there pans the graph, hiding the tooltip, as does scrolling the page. The chart's
 * touch slop is then the system's [touchSlop] (see MeasurementChart), so panning starts as in any
 * scrolling view.
 *
 * **Fully zoomed out** (the whole series fits, nothing to pan): the tooltip shows on press and
 * follows the finger across the whole width (scrub). Up/down travel past [touchSlop] hides it — the
 * page is scrolling — unless the finger has already gone [panSlop] sideways: the chart's drag then
 * owns the gesture, so the page can't scroll. The chart's touch slop is [panSlop] in this mode.
 *
 * @param panSlop the chart's touch slop when fully zoomed out, in px.
 * @param touchSlop the system touch slop in px: where the page scrolls, and the zoomed-in chart pans.
 */
internal class ScrubUntilPanController(
  private val panSlop: Float,
  private val touchSlop: Float,
  private val canPan: () -> Boolean,
) : CartesianMarkerController {
  private var down: Point? = null
  private var panning = false
  private var scrubOnly = false
  /** [scrubOnly] and past [panSlop] sideways: the chart's drag owns the gesture. */
  private var scrubbing = false
  /** Zoomed in: the finger was held long enough to ask for the tooltip. */
  private var held = false

  // Vico sends LongPress only when asked; it's how "hold" reaches us when zoomed in.
  override val acceptsLongPress = true
  // The tooltip stays under the finger, as Vico's show-on-press.
  override val lock = CartesianMarkerController.Lock.Position

  override fun shouldAcceptInteraction(
    interaction: Interaction,
    targets: List<CartesianMarker.Target>
  ) =
    when (interaction) {
      is Interaction.Press -> true
      is Interaction.Move, is Interaction.Release, is Interaction.LongPress -> down != null
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
        scrubOnly = !canPan()
        scrubbing = false
        held = false
      }

      is Interaction.LongPress -> held = true

      is Interaction.Release -> {
        down = null
        return false
      }

      else -> {
        val start = down ?: return false
        val upOrDown = abs(interaction.point.y - start.y) > touchSlop
        if (scrubOnly) {
          val sideways = abs(interaction.point.x - start.x) > panSlop
          if (!scrubbing && upOrDown) panning = true
          if (!panning && sideways) scrubbing = true
        } else if (upOrDown || abs(interaction.point.x - start.x) > touchSlop) {
          panning = true
        }
      }
    }
    return !panning && (scrubOnly || held)
  }
}
