package pl.llp.aircasting.ui.chart

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
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
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions
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
import com.patrykandpatrick.vico.compose.common.DashedShape
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.LineComponent
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

/** Seconds visible at first: the design frame spans ~8 h (19:00–03:00). Open with design: see EXPLORE_SCREEN.md. */
private const val INITIAL_VISIBLE_SECONDS = 8 * 3_600.0

/** How far sideways (from where the finger landed) the tooltip scrubs before the chart pans. System default: 8 dp. */
private val PanSlop = 48.dp
/** The system configuration with the chart's own touch slop. */
private class ChartViewConfiguration(system: ViewConfiguration, override val touchSlop: Float) :
  ViewConfiguration by system
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
  val midnightLine = remember {
    LineComponent(
      fill = Fill(MidnightLineColor),
      thickness = 1.dp,
      shape = DashedShape(dashLength = 8.dp, gapLength = 4.dp),
    )
  }
  val decorations = remember(threshold, points, midnightLine) {
    threshold?.let { bands(it, AqBandColors) }.orEmpty() +
      MidnightLines(midnightsBetween(points.first().x, points.last().x), midnightLine) +
      visibleRange
  }
  val firstX = points.first().x
  val timeLabels = remember { TimeAxisLabels() }
  val labelColor = colors.onSurfaceVariant
  val system = LocalViewConfiguration.current
  val panSlop = with(LocalDensity.current) { PanSlop.toPx() }
  val chartConfiguration = remember(system, panSlop) { ChartViewConfiguration(system, panSlop) }
  val scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End)
  val markerController = remember(panSlop, system.touchSlop, scrollState) {
    ScrubUntilPanController(panSlop, system.touchSlop, canPan = { scrollState.maxValue >= 1f })
  }
  val zoomState = rememberVicoZoomState(
    initialZoom = remember { Zoom.max(Zoom.x(INITIAL_VISIBLE_SECONDS), Zoom.Content) },
    minZoom = Zoom.Content,
    // A series shorter than MIN_VISIBLE_SECONDS already fits at Content; Zoom.x alone would then sit
    // below minZoom, which Vico rejects (IllegalArgumentException). max() keeps maxZoom ≥ minZoom.
    maxZoom = remember { Zoom.max(Zoom.x(MIN_VISIBLE_SECONDS), Zoom.Content) },
  )
  CompositionLocalProvider(LocalViewConfiguration provides chartConfiguration) {
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
            CartesianLayerRangeProvider.fixed(
              minX = firstX,
              maxX = points.last().x,
              minY = y.start,
              maxY = y.endInclusive
            )
          },
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
          line = null,
          tick = null,
          guideline = null,
          label = rememberAxisLabelComponent(style = MaterialTheme.typography.labelSmall.copy(color = labelColor)),
          valueFormatter = timeLabels,
          itemPlacer = timeLabels,
        ),
        marker = rememberDefaultCartesianMarker(
          label = rememberTextComponent(
            style = typography.labelSmall.copy(color = labelColor), // line 1 (time) and the unit
            lineCount = 2,
            padding = Insets(8.dp),
            background = remember(colors.background) {
              ShapeComponent(
                fill = Fill(colors.background),
                shape = RoundedCornerShape(8.dp),
                shadows = ElevationLight1
              )
            },
          ),
          valueFormatter = remember(unit, threshold, aq, typography, colors) {
            MarkerFormatter(
              unit,
              threshold,
              aq,
              valueStyle = typography.titleSmallEmphasized.toSpanStyle()
                .copy(color = colors.onBackground),
            )
          },
          labelPosition = DefaultCartesianMarker.LabelPosition.AbovePoint,
          indicator = remember(colors.primaryContainer) {
            { _: Color ->
              ShapeComponent(
                fill = Fill(colors.primaryContainer),
                shape = CircleShape
              )
            }
          },
          indicatorSize = 12.dp,
          guideline = rememberLineComponent(fill = Fill(Color.White), thickness = 1.dp),
        ),
        decorations = decorations,
        markerController = markerController,
        // One x unit = one second. The default x-step is the GCD of the loaded x deltas, which would
        // change from window to window (1-min data: 60) and make the zoom jump on every reload.
        getXStep = { _, _, _ -> 1.0 },
      ),
      modelProducer = modelProducer,
      modifier = modifier.pinchToZoom(zoomState),
      scrollState = scrollState,
      zoomState = zoomState,
      // Diff animations interpolate every point on each reload: a stall at these densities.
      animationSpec = null,
      animateIn = false,
    )
  }
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

/** One background band per level, from the user's threshold. */
private fun bands(threshold: SensorThreshold, aq: AqColors): List<Decoration> = threshold.bands.map { (range, level) ->
  val y = range.first.toDouble()..range.last.toDouble()
  UnderLayers(HorizontalBox(y = { y }, box = ShapeComponent(fill = Fill(aq.forLevel(level))), labelComponent = null))
}

/** `HorizontalBox` only draws over the layers, hiding the line; this re-points it under them. */
private class UnderLayers(private val delegate: Decoration) : Decoration {
  override fun drawUnderLayers(context: CartesianDrawingContext) = delegate.drawOverLayers(context)
}

/**
 * Edge-first time labels (see [timeAxisLabels]): both the axis's item placer and its formatter,
 * because an edge label sits half a label in from the edge yet shows the edge's time, so the
 * formatter needs the placer's last layout to know what to print at that x.
 *
 * Reserves no layer padding (unlike Vico's aligned placer), so the line runs edge to edge.
 */
private class TimeAxisLabels : HorizontalAxis.ItemPlacer, CartesianValueFormatter {
  /** From the latest [getLabelValues], which Vico calls just before formatting what it returns. */
  private var shownTimes = emptyMap<Double, Double>()

  override fun getLabelValues(
    context: CartesianDrawingContext,
    visibleXRange: ClosedFloatingPointRange<Double>,
    fullXRange: ClosedFloatingPointRange<Double>,
    maxLabelWidth: Float,
  ): List<Double> {
    val labelWidth = maxLabelWidth / context.layerDimensions.xSpacing * context.ranges.xStep
    shownTimes = timeAxisLabels(visibleXRange, labelWidth)
    return shownTimes.keys.toList()
  }

  override fun format(context: CartesianMeasuringContext, value: Double, verticalAxisPosition: Axis.Position.Vertical?) =
    hourMinuteUtc(shownTimes[value] ?: value)

  /** `00:00`, `01:01` … `09:09`: every digit in both last places, so the widest label whatever the font. */
  private val measuredValues = List(10) { it * 3_660.0 }

  override fun getWidthMeasurementLabelValues(
    context: CartesianMeasuringContext,
    layerDimensions: CartesianLayerDimensions,
    fullXRange: ClosedFloatingPointRange<Double>,
  ) = measuredValues

  override fun getHeightMeasurementLabelValues(
    context: CartesianMeasuringContext,
    layerDimensions: CartesianLayerDimensions,
    fullXRange: ClosedFloatingPointRange<Double>,
    maxLabelWidth: Float,
  ) = measuredValues

  // No ticks, so no margin for them.
  override fun getStartLayerMargin(
    context: CartesianMeasuringContext,
    layerDimensions: CartesianLayerDimensions,
    tickThickness: Float,
    maxLabelWidth: Float,
  ) = 0f

  override fun getEndLayerMargin(
    context: CartesianMeasuringContext,
    layerDimensions: CartesianLayerDimensions,
    tickThickness: Float,
    maxLabelWidth: Float,
  ) = 0f
}

val ElevationLight1 = listOf(
  Shadow(radius = 3.dp, color = Color(0x26000000), offset = DpOffset(0.dp, 1.dp), spread = 1.dp),
  Shadow(radius = 2.dp, color = Color(0x4D000000), offset = DpOffset(0.dp, 1.dp)),
)

private class MarkerFormatter(
  private val unit: String,
  private val threshold: SensorThreshold?,
  private val aq: AqColors,
  private val valueStyle: SpanStyle,
) : DefaultCartesianMarker.ValueFormatter {
  override fun format(context: CartesianDrawingContext, targets: List<CartesianMarker.Target>): CharSequence {
    val entry = (targets.first() as LineCartesianLayerMarkerTarget).points.first().entry
    return buildAnnotatedString {
      append(hourMinuteSecondUtc(entry.x))
      append('\n')
      threshold?.let { withStyle(SpanStyle(color = aq.forLevel(it.levelFor(entry.y)))) { append("● ") } }
      withStyle(valueStyle) { append("${entry.y.roundToInt()}") }
      append(" $unit")
    }
  }
}

/** Dashed vertical line at each midnight, under the line (legacy GraphContainer.midnightPointLine). */
private class MidnightLines(private val xs: List<Double>, private val line: LineComponent) : Decoration {
  override fun drawUnderLayers(context: CartesianDrawingContext) {
    with(context) {
      // Same x → pixel mapping as LineCartesianLayer.getDrawX, so the line meets the data exactly.
      val drawingStart = (if (isLtr) layerBounds.left else layerBounds.right) +
        layoutDirectionMultiplier * layerDimensions.startPadding - scroll
      xs.forEach { x ->
        val px = drawingStart +
          layoutDirectionMultiplier * layerDimensions.xSpacing * ((x - ranges.minX) / ranges.xStep).toFloat()
        if (px in layerBounds.left..layerBounds.right) {
          line.drawVertical(this, px, layerBounds.top, layerBounds.bottom)
        }
      }
    }
  }
}

private val MidnightLineColor = Color(0xFF737578)
