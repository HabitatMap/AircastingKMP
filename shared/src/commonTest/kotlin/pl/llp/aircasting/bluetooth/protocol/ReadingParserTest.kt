package pl.llp.aircasting.bluetooth.protocol

import pl.llp.aircasting.bluetooth.SensorReading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ReadingParserTest {

  // --- AB2 / AB3 / Mini V1: one ASCII line per reading (legacy `ResponseParser`) ---

  @Test
  fun standard_line_maps_to_sensor_name_unit_and_value() {
    val line = "12.5;AirBeam3:00189610719F;AirBeam3-PM2.5;Particulate Matter;PM;" +
      "micrograms per cubic meter;µg/m³;0;12;35;55;150"

    assertEquals(
      SensorReading("AirBeam3-PM2.5", "µg/m³", 12.5, measuredAt = null),
      AirBeamProtocol.Standard.parseReading(line),
    )
  }

  /** AB2 lines come off a socket with the line terminator still attached. */
  @Test
  fun standard_line_ignores_surrounding_whitespace() {
    val line = "71;AirBeam2:0018961070D6;AirBeam2-F;Temperature;F;degrees Fahrenheit;F;15;45;75;105;135\r\n"

    assertEquals(71.0, AirBeamProtocol.Standard.parseReading(line)?.value)
  }

  /** Mini V1 sends its battery level ("85%") on a measurement characteristic. */
  @Test
  fun standard_battery_notification_is_not_a_reading() {
    assertNull(AirBeamProtocol.Standard.parseReading("85%"))
  }

  @Test
  fun standard_line_with_missing_fields_is_dropped() {
    assertNull(AirBeamProtocol.Standard.parseReading("12.5;AirBeam3:00189610719F;AirBeam3-PM2.5"))
  }

  @Test
  fun standard_line_with_a_non_numeric_value_is_dropped() {
    val line = "nan?;AirBeam3:00189610719F;AirBeam3-PM2.5;Particulate Matter;PM;" +
      "micrograms per cubic meter;µg/m³;0;12;35;55;150"

    assertNull(AirBeamProtocol.Standard.parseReading(line))
  }

  // --- Mini V2: binary, little-endian, device-timestamped ---

  /** `[count_u8=1, timestamp_u32_LE, pm1_u16_LE, pm2_5_u16_LE]` → one reading per sensor. */
  @Test
  fun v2_live_measurement_yields_pm1_and_pm25_at_the_device_time() {
    val bytes = byteArrayOf(1) + u32(1_700_000_000) + u16(7) + u16(300)
    val at = Instant.fromEpochSeconds(1_700_000_000)

    assertEquals(
      listOf(
        SensorReading("AirBeamMini-PM1", "µg/m³", 7.0, at),
        SensorReading("AirBeamMini-PM2.5", "µg/m³", 300.0, at),
      ),
      AirBeamProtocol.MiniV2.parseMeasurement(bytes),
    )
  }

  /** Unsigned on the wire: 0xFFFF is 65535, not -1. */
  @Test
  fun v2_values_are_unsigned() {
    val bytes = byteArrayOf(1) + u32(1_700_000_000) + u16(0xFFFF) + u16(0)

    assertEquals(65535.0, AirBeamProtocol.MiniV2.parseMeasurement(bytes).first().value)
  }

  @Test
  fun v2_short_measurement_is_dropped() {
    assertEquals(emptyList(), AirBeamProtocol.MiniV2.parseMeasurement(byteArrayOf(1, 2, 3)))
  }

  /** `[count_u8, padding_2B, (timestamp_u32, pm1_u16, pm2_5_u16) × count]`. */
  @Test
  fun v2_sync_chunk_yields_every_record_in_order() {
    val bytes = byteArrayOf(2, 0, 0) +
      u32(100) + u16(1) + u16(2) +
      u32(101) + u16(3) + u16(4)

    assertEquals(
      listOf(
        SensorReading("AirBeamMini-PM1", "µg/m³", 1.0, Instant.fromEpochSeconds(100)),
        SensorReading("AirBeamMini-PM2.5", "µg/m³", 2.0, Instant.fromEpochSeconds(100)),
        SensorReading("AirBeamMini-PM1", "µg/m³", 3.0, Instant.fromEpochSeconds(101)),
        SensorReading("AirBeamMini-PM2.5", "µg/m³", 4.0, Instant.fromEpochSeconds(101)),
      ),
      AirBeamProtocol.MiniV2.parseSyncChunk(bytes),
    )
  }

  /** A chunk cut short keeps the records that arrived whole. */
  @Test
  fun v2_sync_chunk_shorter_than_its_count_keeps_whole_records_only() {
    val bytes = byteArrayOf(3, 0, 0) + u32(100) + u16(1) + u16(2) + u32(101)

    assertEquals(2, AirBeamProtocol.MiniV2.parseSyncChunk(bytes).size)
  }

  private fun u32(v: Long) = ByteArray(4) { i -> (v shr (8 * i)).toByte() }
  private fun u16(v: Int) = byteArrayOf(v.toByte(), (v shr 8).toByte())
}
