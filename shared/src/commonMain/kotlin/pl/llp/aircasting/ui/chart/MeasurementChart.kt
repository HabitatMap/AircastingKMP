package pl.llp.aircasting.ui.chart

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.Scroll
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
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.ui.theme.AqBandColors
import pl.llp.aircasting.ui.theme.AqColors
import pl.llp.aircasting.ui.theme.LocalAqColors
import kotlin.math.roundToInt
import kotlin.collections.orEmpty

/** Seconds visible at first: the design frame spans ~8 h (19:00–03:00). Open with design: see EXPLORE_SCREEN.md. */
private const val INITIAL_VISIBLE_SECONDS = 8 * 3_600.0
/** Furthest zoom-in. */
private const val MIN_VISIBLE_SECONDS = 15 * 60.0

/**
 * A measurement line over threshold-coloured bands, opened at the newest point, pannable and
 * x-zoomable over all of [points]. Only the visible window, decimated, reaches Vico (ChartWindow.kt).
 *
 * @param points sorted by x (epoch seconds, local-as-UTC); not empty.
 * @param threshold null = no bands, as the uncoloured header.
 */
@Composable
fun MeasurementChart(
  points: List<ChartPoint>,
  threshold: SensorThreshold?,
  unit: String,
  modifier: Modifier = Modifier,
) {
  val modelProducer = remember { CartesianChartModelProducer() }
  val visibleRange = remember { VisibleRangeProbe() }

  LaunchedEffect(points) {
    // Before the first frame there is no model, so nothing is drawn and the probe can't report:
    // seed with the range the chart opens on (scrolled to the end).
    visibleRange.value.value = (points.last().x - INITIAL_VISIBLE_SECONDS)..points.last().x
    var loaded: ClosedFloatingPointRange<Double>? = null
    // conflate: while one reload computes, keep only the newest range, so a fling never queues work.
    visibleRange.value.filterNotNull().conflate().collect { visible ->
      if (loaded?.let { needsReload(it, visible) } == false) return@collect
      val window = windowFor(visible)
      val draw = withContext(Dispatchers.Default) { pointsToDraw(points, window) }
      modelProducer.runTransaction { lineModel { series(draw.map { it.x }, draw.map { it.y }) } }
      loaded = window
    }
  }

  val aq = LocalAqColors.current
  val colors = MaterialTheme.colorScheme
  val typography = MaterialTheme.typography
  val y = remember(points, threshold) { yRange(points, threshold) }
  val decorations = remember(threshold) {
    threshold?.let { bands(it, AqBandColors) }.orEmpty() + visibleRange
  }
  val firstX = points.first().x
  val labelColor = colors.onSurfaceVariant
  CartesianChartHost(
    chart = rememberCartesianChart(
      rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
          LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(
              Fill(if (threshold != null) Color.White else MaterialTheme.colorScheme.onSurface),
            ),
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp),
            interpolator = LineCartesianLayer.Interpolator.Sharp,
          ),
        ),
        // x fixed to the whole series, though the model only holds a window: that keeps the full 24 h
        // scrollable. y fixed so it doesn't rescale as windows swap (see yRange).
        rangeProvider = remember(points, y) {
          CartesianLayerRangeProvider.fixed(minX = firstX, maxX = points.last().x, minY = y.start, maxY = y.endInclusive)
        },
      ),
      bottomAxis = HorizontalAxis.rememberBottom(
        line = null,
        tick = null,
        guideline = null,
        label = rememberAxisLabelComponent(style = MaterialTheme.typography.labelSmall.copy(color = labelColor)),
        valueFormatter = remember { TimeFormatter },
        // A label per full hour; Vico skips some (every 2 h, 3 h…) when they'd overlap.
        itemPlacer = remember(firstX) {
          HorizontalAxis.ItemPlacer.aligned(spacing = { 3_600 }, offset = { secondsToNextHour(firstX) })
        },
      ),
      // Figma tooltip: a Background-coloured card with Elevation Light/1, white guideline, blue dot.
      marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
          style = typography.labelSmall.copy(color = labelColor), // line 1 (time) and the unit
          lineCount = 2,
          padding = Insets(8.dp),
          background = remember(colors.background) {
            ShapeComponent(fill = Fill(colors.background), shape = RoundedCornerShape(8.dp), shadows = ElevationLight1)
          },
        ),
        valueFormatter = remember(unit, threshold, aq, typography, colors) {
          MarkerFormatter(
            unit,
            threshold,
            aq,
            valueStyle = typography.titleSmallEmphasized.toSpanStyle().copy(color = colors.onBackground),
          )
        },
        labelPosition = DefaultCartesianMarker.LabelPosition.AbovePoint,
        indicator = remember(colors.primaryContainer) {
          { _: Color -> ShapeComponent(fill = Fill(colors.primaryContainer), shape = CircleShape) }
        },
        indicatorSize = 12.dp,
        guideline = rememberLineComponent(fill = Fill(Color.White), thickness = 1.dp),
      ),
      decorations = decorations,
      // One x unit = one second. The default x-step is the GCD of the loaded x deltas, which would
      // change from window to window (1-min data: 60) and make the zoom jump on every reload.
      getXStep = { _, _, _ -> 1.0 },
    ),
    modelProducer = modelProducer,
    modifier = modifier,
    scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
    zoomState = rememberVicoZoomState(
      initialZoom = remember { Zoom.max(Zoom.x(INITIAL_VISIBLE_SECONDS), Zoom.Content) },
      minZoom = Zoom.Content,
      maxZoom = remember { Zoom.x(MIN_VISIBLE_SECONDS) },
    ),
    // Diff animations interpolate every point on each reload: a stall at these densities.
    animationSpec = null,
    animateIn = false,
  )
}

/**
 * Reports the visible x-range on every frame. Vico has no listener for it (its `getVisibleXRange`
 * is internal), so this is that function's maths, done in a decoration where the values are public.
 * Draws nothing.
 */
private class VisibleRangeProbe : Decoration {
  val value = MutableStateFlow<ClosedFloatingPointRange<Double>?>(null)

  override fun drawOverLayers(context: CartesianDrawingContext) {
    with(context) {
      val xPerPixel = ranges.xStep / layerDimensions.xSpacing
      val start = ranges.minX + (layoutDirectionMultiplier * scroll - layerDimensions.startPadding) * xPerPixel
      value.value = start..(start + layerBounds.width * xPerPixel)
    }
  }
}

/** One background band per level, from the user's threshold — the spike's bands. */
private fun bands(threshold: SensorThreshold, aq: AqColors): List<Decoration> = listOf(
  Triple(threshold.veryLow, threshold.low, MeasurementLevel.LOW),
  Triple(threshold.low, threshold.medium, MeasurementLevel.MEDIUM),
  Triple(threshold.medium, threshold.high, MeasurementLevel.HIGH),
  Triple(threshold.high, threshold.veryHigh, MeasurementLevel.VERY_HIGH),
).map { (from, to, level) ->
  val range = from.toDouble()..to.toDouble()
  UnderLayers(HorizontalBox(y = { range }, box = ShapeComponent(fill = Fill(aq.forLevel(level))), labelComponent = null))
}

/** `HorizontalBox` only draws over the layers, hiding the line; this re-points it under them. */
private class UnderLayers(private val delegate: Decoration) : Decoration {
  override fun drawUnderLayers(context: CartesianDrawingContext) = delegate.drawOverLayers(context)
}

private object TimeFormatter : CartesianValueFormatter {
  override fun format(context: CartesianMeasuringContext, value: Double, verticalAxisPosition: Axis.Position.Vertical?) =
    hourMinuteUtc(value)
}

/** Figma `M3/Elevation Light/1`. */
private val ElevationLight1 = listOf(
  Shadow(radius = 3.dp, color = Color(0x26000000), offset = DpOffset(0.dp, 1.dp), spread = 1.dp),
  Shadow(radius = 2.dp, color = Color(0x4D000000), offset = DpOffset(0.dp, 1.dp)),
)

/**
 * Two lines, as Figma: `01:00`, then `● 13 µg/m³` — the dot in the value's level colour (hidden
 * without a threshold), the number in Title Small Emphasized. The rest uses the label's Label Small.
 */
private class MarkerFormatter(
  private val unit: String,
  private val threshold: SensorThreshold?,
  private val aq: AqColors,
  private val valueStyle: SpanStyle,
) : DefaultCartesianMarker.ValueFormatter {
  override fun format(context: CartesianDrawingContext, targets: List<CartesianMarker.Target>): CharSequence {
    val entry = (targets.first() as LineCartesianLayerMarkerTarget).points.first().entry
    return buildAnnotatedString {
      append(hourMinuteUtc(entry.x))
      append('\n')
      threshold?.let { withStyle(SpanStyle(color = aq.forLevel(it.levelFor(entry.y)))) { append("● ") } }
      withStyle(valueStyle) { append("${entry.y.roundToInt()}") }
      append(" $unit")
    }
  }
}