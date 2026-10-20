package pl.llp.aircasting.bluetooth

import kotlin.time.Instant

/**
 * One value from one sensor, as the device sent it. Not yet a stored `Measurement`: it has no
 * stream id and no location — the recording side adds both.
 */
data class SensorReading(
  /** Stream identity on the backend, e.g. `AirBeam3-PM2.5`. */
  val sensorName: String,
  val unitSymbol: String,
  val value: Double,
  /** Device clock (Mini V2). `null` when the protocol carries no time (V1) — stamp on receipt. */
  val measuredAt: Instant?,
)
