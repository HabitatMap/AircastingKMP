package pl.llp.aircasting.mydata

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.Thresholds
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.recording.PhoneMicDeviceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class PastSessionCardTest {
  private val warsaw = TimeZone.of("Europe/Warsaw")

  // 2026-11-23 13:34 in Warsaw (UTC+1).
  private val start = Instant.parse("2026-11-23T12:34:00Z")

  private val thresholds = Thresholds(
    defaults = listOf(SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)),
    aliases = mapOf("AirBeamMini-PM2.5" to "AirBeam-PM2.5"),
  )

  private fun past(
    deviceId: String? = "mini-1",
    streams: List<StreamSummary> = emptyList(),
  ) = PastSession(
    Session(
      uuid = "s1",
      type = SessionType.MOBILE,
      status = SessionStatus.FINISHED,
      name = "Walk",
      tags = listOf("Home"),
      startTime = start,
      endTime = start + 6.hours,
      timeZone = "Europe/Warsaw",
      measurementInterval = 1.seconds,
      deviceId = deviceId,
      deviceModel = "AirBeamMini",
    ),
    streams,
  )

  @Test
  fun streams_come_in_chip_order_with_pm25_selected_and_each_aggregate_graded() {
    val card = pastSessionCard(
      past(
        streams = listOf(
          StreamSummary("AirBeamMini-F", "F", 70.0, 68.0, 72.0),
          StreamSummary("AirBeamMini-PM2.5", "µg/m³", avg = 12.0, min = 3.0, max = 40.0),
          StreamSummary("AirBeamMini-PM1", "µg/m³", 2.0, 1.0, 5.0),
        ),
      ),
      thresholds,
    )

    assertEquals(listOf("AirBeamMini-PM1", "AirBeamMini-PM2.5", "AirBeamMini-F"), card.streams.map { it.sensorName })
    val pm25 = card.selected!!
    assertEquals("AirBeamMini-PM2.5", pm25.sensorName)
    assertEquals(MeasurementLevel.MEDIUM, pm25.avg.level)
    assertEquals(MeasurementLevel.LOW, pm25.min.level)
    assertEquals(MeasurementLevel.HIGH, pm25.max.level)
    assertNull(card.streams.first().avg.level, "no thresholds for PM1 here: grey")
  }

  @Test
  fun a_saved_chip_wins_over_pm25() {
    val card = pastSessionCard(
      past(streams = listOf(StreamSummary("AirBeamMini-PM1", "µg/m³"), StreamSummary("AirBeamMini-PM2.5", "µg/m³"))),
      thresholds,
      savedSensor = "AirBeamMini-PM1",
    )
    assertEquals("AirBeamMini-PM1", card.selected?.sensorName)
  }

  @Test
  fun a_stream_without_readings_has_no_values_or_levels() {
    val stream = pastSessionCard(past(streams = listOf(StreamSummary("AirBeamMini-PM2.5", "µg/m³"))), thresholds)
      .selected!!
    assertEquals(SummaryValue(null, null), stream.avg)
  }

  @Test
  fun phone_mic_sessions_are_told_apart_by_device_id() {
    assertTrue(pastSessionCard(past(deviceId = PhoneMicDeviceId), thresholds).isPhoneMic)
    assertFalse(pastSessionCard(past(), thresholds).isPhoneMic)
  }
}
