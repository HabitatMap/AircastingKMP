package pl.llp.aircasting.map

import pl.llp.aircasting.domain.SensorThreshold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** AirBeam PM2.5 default bands, as used by the legacy app and the web app. */
val pm25Threshold = SensorThreshold(
  sensorName = "AirBeam-PM2.5",
  veryLow = 0,
  low = 12,
  medium = 35,
  high = 55,
  veryHigh = 150,
)

/**
 * Deterministic synthetic mobile session — a 1 Hz walk with heading drift and standing-still
 * pauses, so the dot cloud clumps the way a real recording does.
 *
 * Seeded LCG rather than `kotlin.random.Random` so every run and both platforms produce the
 * exact same track; perf numbers stay comparable between measurements.
 */
fun generateTrack(
  pointCount: Int,
  startLatitude: Double = 40.7205,   // Lower East Side, matching the reference screenshot
  startLongitude: Double = -73.9865,
  seed: Long = 42L,
): Track {
  val latitude = DoubleArray(pointCount)
  val longitude = DoubleArray(pointCount)
  val value = DoubleArray(pointCount)

  var rng = seed
  fun next(): Double {
    rng = (rng * 6364136223846793005L + 1442695040888963407L)
    return ((rng ushr 11).toDouble() / (1L shl 53).toDouble())
  }

  val metersPerDegLat = 111_320.0
  val metersPerDegLng = metersPerDegLat * cos(startLatitude * PI / 180.0)

  var lat = startLatitude
  var lng = startLongitude
  var heading = next() * 2 * PI
  var pollution = 18.0
  var pauseFramesLeft = 0

  for (i in 0 until pointCount) {
    if (pauseFramesLeft > 0) {
      pauseFramesLeft--
    } else {
      if (next() < 0.004) pauseFramesLeft = 20 + (next() * 80).toInt()  // stop at a light etc.
      heading += (next() - 0.5) * 0.35
      val metersPerSecond = 1.1 + next() * 0.6
      lat += sin(heading) * metersPerSecond / metersPerDegLat
      lng += cos(heading) * metersPerSecond / metersPerDegLng
    }
    // GPS jitter — this is what makes "standing still" a blob rather than one point.
    val jitteredLat = lat + (next() - 0.5) * 3.0 / metersPerDegLat
    val jitteredLng = lng + (next() - 0.5) * 3.0 / metersPerDegLng

    pollution += (next() - 0.5) * 3.0
    if (next() < 0.002) pollution += 40.0 * next()          // passing truck
    pollution = pollution.coerceIn(1.0, 190.0) * 0.995 + 0.09

    latitude[i] = jitteredLat
    longitude[i] = jitteredLng
    value[i] = pollution
  }

  return Track(latitude, longitude, value)
}
