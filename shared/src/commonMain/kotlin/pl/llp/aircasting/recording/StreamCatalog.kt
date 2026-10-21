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
  // Empty = not catalogued yet; the engine won't start a session it couldn't upload.
  AirBeamDevice.AirBeam2, AirBeamDevice.AirBeam3, AirBeamDevice.Mini.V1 -> emptyList()
}
