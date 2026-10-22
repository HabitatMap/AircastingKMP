package pl.llp.aircasting.recording

import pl.llp.aircasting.bluetooth.AirBeamDevice

/** One stream a session records: its identity on the backend. */
data class StreamSpec(val sensorName: String, val unitSymbol: String)

/**
 * Every stream a mobile session on [device] records. All of them are created at start: v3 has
 * no call to add a stream to an existing session (`API_V3.md`). Names are legacy's, so backend
 * streams line up with sessions the legacy app recorded.
 *
 */
fun streamsFor(device: AirBeamDevice): List<StreamSpec> = when (device) {
  AirBeamDevice.Mini.V2 -> listOf(
    StreamSpec("AirBeamMini-PM1", "µg/m³"),   // legacy V2MobileMeasurementsInserter
    StreamSpec("AirBeamMini-PM2.5", "µg/m³"),
  )
  // Names come from the firmware's line; legacy SD sync (AB3CSVLineParameterHandler) uses the same.
  AirBeamDevice.AirBeam3 -> listOf(
    StreamSpec("AirBeam3-F", "F"),
    StreamSpec("AirBeam3-RH", "%"),
    StreamSpec("AirBeam3-PM1", "µg/m³"),
    StreamSpec("AirBeam3-PM2.5", "µg/m³"),
    StreamSpec("AirBeam3-PM10", "µg/m³"),
  )
  // Hardware run 2026-10-22: what the AB2 firmware sends; same naming as AB3.
  AirBeamDevice.AirBeam2 -> listOf(
    StreamSpec("AirBeam2-F", "F"),
    StreamSpec("AirBeam2-RH", "%"),
    StreamSpec("AirBeam2-PM1", "µg/m³"),
    StreamSpec("AirBeam2-PM2.5", "µg/m³"),
    StreamSpec("AirBeam2-PM10", "µg/m³"),
  )
  // Empty = not catalogued yet; the engine won't start a session it couldn't upload.
  AirBeamDevice.Mini.V1 -> emptyList()
}