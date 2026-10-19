package pl.llp.aircasting.data.network.upload

import pl.llp.aircasting.domain.GeoLocation
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class MobileMeasurementPayloadTest {
  private val frame = MobileFrame(
    time = Instant.parse("2024-01-01T00:00:00Z"), // 1704067200 = 0x65920080
    sensorTypeId = 3,
    value = 12.5,                                  // float32 0x41480000
    location = GeoLocation(latitude = 40.0, longitude = -74.0),
  )

  @Test
  fun encodes_a_single_frame_byte_for_byte() {
    val expected = bytes(
      0xAB, 0xBA,                                     // magic
      0x00, 0x01,                                     // count = 1
      0x65, 0x92, 0x00, 0x80,                         // timestamp, uint32 BE
      0x03,                                           // sensor_type_id
      0x41, 0x48, 0x00, 0x00,                         // value, float32 BE
      0x40, 0x44, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // latitude, float64 BE
      0xC0, 0x52, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, // longitude, float64 BE
      0x7B,                                           // XOR checksum
    )
    assertContentEquals(expected, MobileMeasurementPayload.encode(listOf(frame)))
  }

  @Test
  fun count_is_big_endian_and_size_is_header_plus_25_bytes_per_frame() {
    val payload = MobileMeasurementPayload.encode(List(300) { frame })

    assertEquals(0x01, payload[2].toInt() and 0xFF) // 300 = 0x012C
    assertEquals(0x2C, payload[3].toInt() and 0xFF)
    assertEquals(4 + 300 * 25 + 1, payload.size)
  }

  @Test
  fun milliseconds_are_truncated_to_whole_seconds() {
    val withMillis = frame.copy(time = Instant.parse("2024-01-01T00:00:00.999Z"))

    assertContentEquals(
      MobileMeasurementPayload.encode(listOf(frame)),
      MobileMeasurementPayload.encode(listOf(withMillis)),
    )
  }

  @Test
  fun empty_list_is_rejected() {
    assertFailsWith<IllegalArgumentException> { MobileMeasurementPayload.encode(emptyList()) }
  }
  @Test
  fun more_than_3000_frames_is_rejected() {
    assertFailsWith<IllegalArgumentException> {
      MobileMeasurementPayload.encode(List(MobileMeasurementPayload.MaxFrames + 1) { frame })
    }
  }

  @Test
  fun sensor_type_id_that_does_not_fit_a_byte_is_rejected() {
    assertFailsWith<IllegalArgumentException> {
      MobileMeasurementPayload.encode(listOf(frame.copy(sensorTypeId = 256)))
    }
  }

  private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}