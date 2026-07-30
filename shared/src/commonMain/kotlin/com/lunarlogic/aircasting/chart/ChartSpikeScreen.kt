package com.lunarlogic.aircasting.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.lunarlogic.aircasting.domain.SensorThreshold
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import kotlin.random.Random
import kotlinx.coroutines.launch

// TODO(spike): placeholder PM2.5 bands. Confirm the real pollutant and boundaries before
//  anything ships.
private val Pm25Threshold = SensorThreshold(
  sensorName = "AirBeam3-PM2.5",
  veryLow = 0,
  low = 12,
  medium = 35,
  high = 55,
  veryHigh = 150,
)

private val PointCounts = listOf(1_000, 10_000, 100_000, 500_000)

/** Points we allow through to Vico when decimation is on — roughly one per horizontal pixel. */
private const val DECIMATION_BUDGET = 800

/**
 * Frame budget of the device under test, plus a small margin. A frame that lands exactly on the
 * budget (11.1 ms at 90 Hz) is on time, so comparing against the bare budget reports 100% jank on
 * a perfectly smooth chart. 12.0 = 90 Hz; use 18.0 for 60 Hz, 9.0 for 120 Hz.
 */
private const val JANK_THRESHOLD_MS = 12.0

/**
 * Stress-test harness for the Vico spike. Not production UI — deliberately crude, revert when the
 * spike concludes.
 *
 * What to measure:
 *  1. baseline: 1k raw, p95 should equal the refresh interval.
 *  2. decimation efficacy: 500k raw vs 500k decimated, both flung at the series start.
 *  3. scroll-position hypothesis: 500k *decimated* (identical viewport density everywhere), then
 *     compare "start" vs "end". Vico's `getSliceIndices` finds the first visible point with a
 *     linear scan from index 0, twice per frame, so cost should grow with scroll position.
 *  4. custom drawing: does [VerticalDashedLine] render dashed and trace-aligned on both platforms
 *     with no androidMain/iosMain file added?
 */
@Composable
fun ChartSpikeScreen() {
  var pointCount by remember { mutableIntStateOf(10_000) }
  var decimate by remember { mutableStateOf(true) }
  val scrollState = rememberVicoScrollState()
  // Gated on the scroll value so only frames that moved the viewport are timed.
  val stats = rememberFrameStats(
    jankThresholdMs = JANK_THRESHOLD_MS,
    activitySignal = { scrollState.value },
  )
  val scope = rememberCoroutineScope()
  val log = remember { Logger.withTag("ChartSpike") }

  val raw = remember(pointCount) { generateWalk(pointCount, Pm25Threshold) }
  val points = remember(raw, decimate) {
    if (decimate) lttb(raw, DECIMATION_BUDGET) else raw
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text("Vico 3.2.3 stress spike", style = MaterialTheme.typography.titleMedium)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      PointCounts.forEach { count ->
        FilterChip(
          selected = pointCount == count,
          onClick = {
            pointCount = count
            stats.reset()
          },
          label = { Text(if (count >= 1000) "${count / 1000}k" else "$count") },
        )
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = decimate,
        onClick = {
          decimate = !decimate
          stats.reset()
        },
        label = { Text(if (decimate) "decimated → ${points.size}" else "raw → ${points.size}") },
      )
      // The scroll-position probe.
      Button(onClick = {
        stats.reset()
        scope.launch { scrollState.scroll(Scroll.Absolute.Start) }
      }) { Text("start") }
      Button(onClick = {
        stats.reset()
        scope.launch { scrollState.scroll(Scroll.Absolute.End) }
      }) { Text("end") }
    }

    MeasurementChart(
      points = points,
      threshold = Pm25Threshold,
      // Inside the initial 60-unit viewport, so the marker is visible without scrolling and
      // scrolls out of view — which also exercises the decoration's bounds check.
      nowX = 40.0,
      scrollState = scrollState,
      // Explicit height: CartesianChartHost wraps itself in heightIn(max = 200.dp), and only an
      // outer fixed height overrides that cap.
      modifier = Modifier.fillMaxWidth().height(260.dp).background(Color(0xFFEFEFEF)),
    )

    Text(
      "p50 ${stats.p50Ms.fmt()} ms · p95 ${stats.p95Ms.fmt()} ms · worst " +
        "${stats.worstMs.fmt()} ms · jank ${stats.jankPercent.fmt()}% · n=${stats.sampleCount}",
      style = MaterialTheme.typography.bodyMedium,
    )
    Text(
      "Scroll frames only · jank = frame > $JANK_THRESHOLD_MS ms · fling then read p95",
      style = MaterialTheme.typography.bodySmall,
    )

    Button(onClick = {
      log.i {
        "count=$pointCount decimated=$decimate fed=${points.size} " +
          "p50=${stats.p50Ms.fmt()} p95=${stats.p95Ms.fmt()} " +
          "worst=${stats.worstMs.fmt()} jank=${stats.jankPercent.fmt()}%"
      }
    }) { Text("log result") }
  }
}

private fun Double.fmt(): String = ((this * 10).toInt() / 10.0).toString()

/** Deterministic random walk clamped to the threshold range — shaped like the reference trace. */
private fun generateWalk(count: Int, threshold: SensorThreshold): List<ChartPoint> {
  val random = Random(seed = 42)
  var value = (threshold.low + threshold.medium) / 2.0
  return List(count) { index ->
    value = (value + random.nextDouble(-1.5, 1.5))
      .coerceIn(threshold.veryLow.toDouble(), threshold.veryHigh.toDouble())
    ChartPoint(x = index.toDouble(), y = value)
  }
}
