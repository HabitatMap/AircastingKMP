package pl.llp.aircasting.recording

import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreamCatalogTest {
  /** Exact legacy names, so backend streams line up with sessions the legacy app recorded. */
  @Test
  fun mini_v2_records_pm1_and_pm25() {
    assertEquals(
      listOf(
        StreamSpec("AirBeamMini-PM1", "µg/m³"),
        StreamSpec("AirBeamMini-PM2.5", "µg/m³"),
      ),
      streamsFor(AirBeamDevice.Mini.V2),
    )
  }

  /** The engine drops a reading with no stream, so the parser and the catalog must agree. */
  @Test
  fun every_mini_v2_reading_has_a_stream() {
    val live = byteArrayOf(1) + ByteArray(8)              // count=1, one 8-byte record
    val sync = byteArrayOf(2, 0, 0) + ByteArray(2 * 8)    // count=2, 2B padding, two records
    val readings = AirBeamProtocol.MiniV2.parseMeasurement(live) +
      AirBeamProtocol.MiniV2.parseSyncChunk(sync)
    assertEquals(6, readings.size) // 2 sensors × 3 records; guards against an empty, vacuous pass

    val catalog = streamsFor(AirBeamDevice.Mini.V2)
    readings.forEach { assertTrue(StreamSpec(it.sensorName, it.unitSymbol) in catalog, "no stream for $it") }
  }
  /** Not catalogued until each gets a hardware run; the engine won't start a session for them. */
  @Test
  fun other_devices_have_no_streams_yet() {
    listOf(AirBeamDevice.AirBeam2, AirBeamDevice.AirBeam3, AirBeamDevice.Mini.V1).forEach {
      assertEquals(emptyList(), streamsFor(it), "$it")
    }
  }
}