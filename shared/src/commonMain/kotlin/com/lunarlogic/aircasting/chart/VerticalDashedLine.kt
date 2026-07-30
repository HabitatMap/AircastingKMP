package com.lunarlogic.aircasting.chart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

/**
 * A [Decoration] drawing a dashed vertical line at an arbitrary _x_ value — the "now" marker.
 *
 * Vico ships only horizontal decorations ([com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine],
 * [com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalBox]), so this is the spike's
 * custom-drawing escape-hatch test. Everything it touches (`Canvas`, `Paint`, `PathEffect`,
 * `Offset`) is `androidx.compose.ui.graphics`, i.e. commonMain: no expect/actual, no platform canvas.
 *
 * Deliberately a `Decoration` rather than a Compose overlay: an overlay would need the same
 * data-x -> canvas-x maths duplicated outside the chart, plus the scroll and zoom values, which
 * Vico only exposes cleanly inside the drawing context.
 */
class VerticalDashedLine(
  private val x: (ExtraStore) -> Double,
  private val color: Color,
  private val thickness: Dp = 2.dp,
  private val dashLength: Dp = 6.dp,
  private val gapLength: Dp = 6.dp,
) : Decoration {

  // Reused across frames: Paint allocation per frame is exactly the kind of cost that shows up
  // as jank at 120 Hz. Vico's own components do the same.
  private val paint = Paint().apply { style = PaintingStyle.Stroke }

  override fun drawOverLayers(context: CartesianDrawingContext) {
    with(context) {
      val target = x(model.extraStore)

      // Same mapping LineCartesianLayer uses for its own points, so the marker cannot drift from
      // the trace: start of the layer, plus start padding, minus scroll, plus xSpacing per x-step.
      // xSpacing is already zoom-scaled by Vico, so zoom needs no term of its own.
      val boundsStart = if (isLtr) layerBounds.left else layerBounds.right
      val canvasX = boundsStart +
        layoutDirectionMultiplier * layerDimensions.startPadding -
        scroll +
        layoutDirectionMultiplier * layerDimensions.xSpacing *
        ((target - ranges.minX) / ranges.xStep).toFloat()

      if (canvasX < layerBounds.left || canvasX > layerBounds.right) return

      paint.color = color
      paint.strokeWidth = thickness.pixels
      paint.pathEffect =
        PathEffect.dashPathEffect(floatArrayOf(dashLength.pixels, gapLength.pixels), 0f)

      canvas.drawLine(
        Offset(canvasX, layerBounds.top),
        Offset(canvasX, layerBounds.bottom),
        paint,
      )
    }
  }
}
