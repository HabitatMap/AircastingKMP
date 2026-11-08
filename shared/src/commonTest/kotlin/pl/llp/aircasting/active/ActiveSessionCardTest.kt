package pl.llp.aircasting.active

import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.Thresholds
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ActiveSessionCardTest {
  private val t0 = Instant.fromEpochMilliseconds(1_786_000_000_000)
  private val thresholds = Thresholds(
    defaults = listOf(SensorThreshold("AirBeam-PM2.5", veryLow = 0, low = 9, medium = 35, high = 55, veryHigh = 150)),
    aliases = mapOf("AirBeamMini-PM2.5" to "AirBeam-PM2.5"),
  )

  private fun session(status: SessionStatus = SessionStatus.RECORDING) = Session(
    uuid = "s1",
    type = SessionType.MOBILE,
    status = status,
    name = "Living room",
    tags = listOf("Home", "Alaska"),
    startTime = t0,
    timeZone = "Europe/Warsaw",
    measurementInterval = 1.seconds,
    deviceModel = "AirBeamMini",
  )

  private fun reading(sensor: String, value: Double? = null, at: Instant? = null) =
    LatestReading(sensor, "µg/m³", value, at)

  private val miniWithData = listOf(
    reading("AirBeamMini-PM1", 3.0, t0 + 5.seconds),
    reading("AirBeamMini-PM2.5", 12.0, t0 + 6.seconds),
  )

  @Test
  fun `recording session with readings is live and last seen at its newest reading`() {
    val card = activeSessionCard(session(), miniWithData, thresholds)

    assertEquals(CardState.Live, card.state)
    assertEquals(t0 + 6.seconds, card.lastSeen)
  }

  @Test
  fun `recording session without readings has no data yet`() {
    val card = activeSessionCard(session(), listOf(reading("AirBeamMini-PM1"), reading("AirBeamMini-PM2.5")), thresholds)

    assertEquals(CardState.NoDataYet, card.state)
    assertNull(card.lastSeen)
  }

  @Test
  fun `disconnected session has lost its connection even with readings`() {
    val card = activeSessionCard(session(SessionStatus.DISCONNECTED), miniWithData, thresholds)

    assertEquals(CardState.ConnectionLost, card.state)
  }

  @Test
  fun `streams follow the design order - PMs first, then temperature and humidity`() {
    // StreamCatalog (and legacy) list F and RH first; Figma 461:2780 puts them last.
    val ab3 = listOf("AirBeam3-F", "AirBeam3-RH", "AirBeam3-PM1", "AirBeam3-PM2.5", "AirBeam3-PM10").map { reading(it) }

    val card = activeSessionCard(session(), ab3, thresholds)

    assertEquals(
      listOf("AirBeam3-PM1", "AirBeam3-PM2.5", "AirBeam3-PM10", "AirBeam3-F", "AirBeam3-RH"),
      card.streams.map { it.sensorName },
    )
  }

  @Test
  fun `PM2_5 is selected by default`() {
    assertEquals("AirBeamMini-PM2.5", activeSessionCard(session(), miniWithData, thresholds).selected?.sensorName)
  }

  @Test
  fun `the saved selection wins over PM2_5`() {
    val card = activeSessionCard(session(), miniWithData, thresholds, savedSensor = "AirBeamMini-PM1")

    assertEquals("AirBeamMini-PM1", card.selected?.sensorName)
  }

  @Test
  fun `a saved selection the session lacks falls back to PM2_5`() {
    val card = activeSessionCard(session(), miniWithData, thresholds, savedSensor = "AirBeam3-RH")

    assertEquals("AirBeamMini-PM2.5", card.selected?.sensorName)
  }

  @Test
  fun `without PM2_5 the first stream in chip order is selected`() {
    val card = activeSessionCard(session(), listOf(reading("AirBeam3-RH"), reading("AirBeam3-F")), thresholds)

    assertEquals("AirBeam3-F", card.selected?.sensorName)
  }

  @Test
  fun `level comes from thresholds through the sensor alias`() {
    val card = activeSessionCard(session(), miniWithData, thresholds)

    // 12 µg/m³ is above low (9), at most medium (35).
    assertEquals(MeasurementLevel.MEDIUM, card.selected?.level)
  }

  @Test
  fun `a stream with no thresholds or no reading has no level`() {
    val card = activeSessionCard(
      session(),
      listOf(reading("AirBeamMini-PM1", 3.0, t0), reading("AirBeamMini-PM2.5")),
      thresholds,
    )

    assertNull(card.streams.single { it.sensorName == "AirBeamMini-PM1" }.level) // no PM1 threshold
    assertNull(card.streams.single { it.sensorName == "AirBeamMini-PM2.5" }.level) // no value
  }
}
