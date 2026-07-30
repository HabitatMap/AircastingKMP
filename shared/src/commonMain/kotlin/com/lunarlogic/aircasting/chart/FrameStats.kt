package com.lunarlogic.aircasting.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

/**
 * Rolling frame-time statistics, published every [PUBLISH_EVERY] frames.
 *
 * Publishing on every frame would be self-defeating: writing observable state each frame forces a
 * recomposition each frame, so the probe would both inflate its own numbers and keep the app
 * rendering when it would otherwise idle.
 *
 * [jankThresholdMs] must match the device's frame budget — 16.7 at 60 Hz, 11.1 at 90 Hz, 8.3 at
 * 120 Hz. A 60 Hz threshold on a 90 Hz panel silently passes frames that actually missed.
 */
@Stable
class FrameStats(private val jankThresholdMs: Double = 16.7) {
  var p50Ms by mutableStateOf(0.0); private set
  var p95Ms by mutableStateOf(0.0); private set
  var worstMs by mutableStateOf(0.0); private set
  var jankPercent by mutableStateOf(0.0); private set
  var sampleCount by mutableStateOf(0); private set

  private val window = ArrayDeque<Double>()
  private var sinceLastPublish = 0

  fun record(deltaMs: Double) {
    // Frames are only produced when there is work, so an idle gap shows up as a huge delta.
    // Those are not jank and must not enter the window.
    if (deltaMs > IDLE_GAP_MS) return

    window.addLast(deltaMs)
    if (window.size > WINDOW) window.removeFirst()
    if (++sinceLastPublish < PUBLISH_EVERY) return
    sinceLastPublish = 0

    val sorted = window.sorted()
    p50Ms = sorted[sorted.size / 2]
    p95Ms = sorted[(sorted.size * 95 / 100).coerceAtMost(sorted.size - 1)]
    worstMs = sorted.last()
    jankPercent = 100.0 * window.count { it > jankThresholdMs } / window.size
    sampleCount = window.size
  }

  fun reset() {
    window.clear()
    sinceLastPublish = 0
    p50Ms = 0.0
    p95Ms = 0.0
    worstMs = 0.0
    jankPercent = 0.0
    sampleCount = 0
  }

  private companion object {
    const val WINDOW = 180
    const val PUBLISH_EVERY = 20
    const val IDLE_GAP_MS = 200.0
  }
}

/**
 * Drives a [FrameStats] from `withFrameNanos`, which is the multiplatform frame clock: Choreographer
 * on Android, CADisplayLink-backed on iOS. No expect/actual needed.
 *
 * [activitySignal] gates sampling: only frames on which it changed are recorded. Pass the chart's
 * scroll value, so the window holds scroll frames exclusively. Without this gate, idle 16.7 ms
 * frames between gestures dilute the percentiles and a chart that stutters badly under the finger
 * reads as smooth.
 */
@Composable
fun rememberFrameStats(jankThresholdMs: Double, activitySignal: () -> Float): FrameStats {
  val stats = remember(jankThresholdMs) { FrameStats(jankThresholdMs) }
  LaunchedEffect(stats) {
    var previousNanos = withFrameNanos { it }
    var previousSignal = activitySignal()
    while (true) {
      val nanos = withFrameNanos { it }
      val signal = activitySignal()
      if (signal != previousSignal) stats.record((nanos - previousNanos) / 1_000_000.0)
      previousNanos = nanos
      previousSignal = signal
    }
  }
  return stats
}
