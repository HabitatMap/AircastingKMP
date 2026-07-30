package com.lunarlogic.aircasting.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunarlogic.aircasting.domain.MeasurementLevel
import com.lunarlogic.aircasting.domain.SensorThreshold
import com.lunarlogic.aircasting.ui.theme.AqColors
import com.lunarlogic.aircasting.ui.theme.LocalAqColors
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.VicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalBox
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent

/**
 * The reference chart: a white measurement trace over four coloured threshold bands, with a
 * time axis and a dashed vertical "now" marker. Scrollable through time, pinch-zoomable on x only.
 */
@Composable
fun MeasurementChart(
  points: List<ChartPoint>,
  threshold: SensorThreshold,
  nowX: Double?,
  scrollState: VicoScrollState,
  modifier: Modifier = Modifier,
  visibleXUnits: Double = 60.0,
) {
  // Held across recompositions — recreating it would discard the model and re-run the transaction
  // every frame. In production this belongs in the ViewModel, not here.
  val modelProducer = remember { CartesianChartModelProducer() }

  // Vico takes Collection<Number>, so this boxes every value. At 500k points that is ~1M objects
  // per transaction — one of the costs the spike is here to quantify.
  LaunchedEffect(points) {
    modelProducer.runTransaction {
      lineModel { series(points.map { it.x }, points.map { it.y }) }
    }
  }

  val aq = LocalAqColors.current
  val decorations = remember(threshold, nowX, aq) {
    buildBands(threshold, aq) + listOfNotNull(
      nowX?.let { x -> VerticalDashedLine(x = { x }, color = Color.Black) }
    )
  }

  CartesianChartHost(
    chart = rememberCartesianChart(
      rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
          LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(Color.White)),
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp),
            // Sharp = straight segments. Cubic interpolation would add per-segment Bezier maths
            // to every frame, which we do not want to pay while measuring baseline throughput.
            interpolator = LineCartesianLayer.Interpolator.Sharp,
          )
        ),
        // Bands only line up with the trace if the y range never moves. Auto-ranging would
        // rescale as you scroll and the coloured backgrounds would slide out from under the line.
        rangeProvider = CartesianLayerRangeProvider.fixed(
          minY = threshold.veryLow.toDouble(),
          maxY = threshold.veryHigh.toDouble(),
        ),
      ),
      bottomAxis = HorizontalAxis.rememberBottom(
        line = null,
        tick = null,
        guideline = null,
        label = rememberAxisLabelComponent(
          style = TextStyle(fontSize = 14.sp, color = Color(0xFF6B7280))
        ),
        valueFormatter = remember { ElapsedTimeFormatter },
        // One label per minute of x-units, matching the reference's 09:51 / 09:52 spacing.
        itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 60 }) },
      ),
      decorations = decorations,
      // No marker: it is the only thing that makes Vico walk the series for hit-testing on tap,
      // and we want the render-cost measurement uncontaminated.
      marker = null,
    ),
    modelProducer = modelProducer,
    modifier = modifier,
    scrollState = scrollState,
    zoomState = rememberVicoZoomState(
      // Zoom.x constrains the *time* axis only: y stays pinned to the threshold range, so the
      // bands never move. This is the pinch-to-zoom requirement, one line.
      initialZoom = remember { Zoom.x(visibleXUnits) },
      minZoom = remember { Zoom.x(visibleXUnits * 20) },
      maxZoom = remember { Zoom.x(visibleXUnits / 20) },
    ),
    // Difference animations interpolate every point on data change. At our densities that is a
    // guaranteed stall, and it would pollute the frame-time numbers.
    animationSpec = null,
    animateIn = false,
  )
}

/** Four `HorizontalBox` decorations, one per level, straight off the project's own threshold. */
private fun buildBands(threshold: SensorThreshold, aq: AqColors): List<Decoration> {
  val bounds = listOf(
    threshold.veryLow to threshold.low,
    threshold.low to threshold.medium,
    threshold.medium to threshold.high,
    threshold.high to threshold.veryHigh,
  )
  val levels = listOf(
    MeasurementLevel.LOW,
    MeasurementLevel.MEDIUM,
    MeasurementLevel.HIGH,
    MeasurementLevel.VERY_HIGH,
  )
  return bounds.mapIndexed { index, (start, end) ->
    val range = start.toDouble()..end.toDouble()
    UnderLayers(
      HorizontalBox(
        y = { range },
        box = ShapeComponent(fill = Fill(aq.forLevel(levels[index]))),
        // Suppresses HorizontalBox's default "12–35" label.
        labelComponent = null,
      )
    )
  }
}

/**
 * Moves a [Decoration] from the over-layers pass to the under-layers pass.
 *
 * `HorizontalBox` only implements `drawOverLayers`, so used directly it paints *on top of* the
 * trace and hides it completely — Vico offers no flag for this. `Decoration` declares both passes,
 * so re-pointing one at the other is all it takes to turn a box into a background band.
 */
private class UnderLayers(private val delegate: Decoration) : Decoration {
  override fun drawUnderLayers(context: CartesianDrawingContext) {
    delegate.drawOverLayers(context)
  }
}

/** Renders x (seconds since session start) as m:ss. */
private object ElapsedTimeFormatter : CartesianValueFormatter {
  override fun format(
    context: CartesianMeasuringContext,
    value: Double,
    verticalAxisPosition: Axis.Position.Vertical?,
  ): CharSequence {
    val total = value.toLong().coerceAtLeast(0L)
    val minutes = total / 60
    val seconds = total % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
  }
}
