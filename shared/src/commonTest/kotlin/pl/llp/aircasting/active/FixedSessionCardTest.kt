package pl.llp.aircasting.active

import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.Thresholds
import pl.llp.aircasting.i18n.EnStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FixedSessionCardTest {
  private val now = Instant.fromEpochMilliseconds(1_786_000_000_000)
  private val thresholds = Thresholds(listOf(SensorThreshold("AirBeam3-PM2.5", 0, 9, 35, 55, 150)), aliases = emptyMap())

  @Test
  fun reported_within_the_offline_window_is_live_and_coloured() {
    val card = activeFixedSessionCard(fixed(reportedAgo = FixedOfflineAfter - 1.seconds), thresholds, now = now)

    assertEquals(CardState.Live, card.state)
    assertEquals("AirBeam3-PM2.5", card.selected?.sensorName)
    assertEquals(MeasurementLevel.MEDIUM, card.selected?.level)
  }

  @Test
  fun no_report_for_longer_than_the_offline_window_is_offline() {
    val card = activeFixedSessionCard(fixed(reportedAgo = FixedOfflineAfter + 1.seconds), thresholds, now = now)

    assertEquals(CardState.Offline, card.state)
  }

  @Test
  fun never_reported_is_no_data_yet() {
    val card = activeFixedSessionCard(fixed(reportedAgo = null), thresholds, now = now)

    assertEquals(CardState.NoDataYet, card.state)
  }

  @Test
  fun meta_starts_with_the_placement() {
    val indoor = activeFixedSessionCard(fixed(reportedAgo = 90.seconds), thresholds, now = now)
    val outdoor = activeFixedSessionCard(fixed(reportedAgo = 90.seconds).copy(isIndoor = false), thresholds, now = now)

    assertEquals(listOf("Indoor", "AirBeam Mini", "1 min ago"), indoor.meta(now, EnStrings))
    assertEquals("Outdoor", outdoor.meta(now, EnStrings).first())
  }

  private fun fixed(reportedAgo: kotlin.time.Duration?) = ActiveFixedSession(
    uuid = "f1",
    name = "Living room",
    tags = listOf("Home"),
    deviceModel = "AirBeamMini",
    isIndoor = true,
    timeZone = "America/New_York",
    startTime = now - 60.minutes,
    lastReportedAt = reportedAgo?.let { now - it },
    readings = listOf(
      LatestReading("AirBeam3-PM1", "µg/m³", reportedAgo?.let { 3.0 }, reportedAgo?.let { now - it }),
      LatestReading("AirBeam3-PM2.5", "µg/m³", reportedAgo?.let { 12.0 }, reportedAgo?.let { now - it }),
    ),
  )
}
