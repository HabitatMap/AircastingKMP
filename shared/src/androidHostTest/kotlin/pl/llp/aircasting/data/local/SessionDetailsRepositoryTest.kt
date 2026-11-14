package pl.llp.aircasting.data.local

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.recording.PhoneMicDeviceId
import pl.llp.aircasting.ui.chart.ChartPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class SessionDetailsRepositoryTest {
  private val db = testDatabase()
  private val sessions = LocalSessionRepository(db.sessionQueries)
  private val streams = LocalMeasurementStreamRepository(db.measurementStreamQueries)
  private val measurements = LocalMeasurementRepository(db.measurementQueries, db.pendingUploadQueries)
  private val repo = LocalSessionDetailsRepository(db.sessionQueries)

  /** 2026-08-05: Warsaw is on CEST, UTC+2. */
  private val t0 = Instant.fromEpochMilliseconds(1_786_000_000_000)
  private val warsaw = TimeZone.of("Europe/Warsaw")

  private fun session(deviceId: String = "00:11:22:33", deviceModel: String = "AirBeamMini") = Session(
    uuid = "s1",
    type = SessionType.MOBILE,
    status = SessionStatus.RECORDING,
    name = "Bike to work",
    tags = listOf("Home", "Alaska"),
    startTime = t0,
    timeZone = "Europe/Warsaw",
    measurementInterval = 1.seconds,
    deviceId = deviceId,
    deviceModel = deviceModel,
    isIndoor = true,
  )

  @Test
  fun `the header comes from the session`() = runTest {
    sessions.insert(session())

    val details = assertNotNull(repo.observe("s1").first())

    assertEquals("s1", details.uuid)
    assertEquals(SessionType.MOBILE, details.type)
    assertEquals(DataSource.AirBeam, details.source)
    assertEquals("Bike to work", details.title)
    assertEquals("AirBeamMini", details.deviceModel)
    assertEquals(listOf("Home", "Alaska"), details.tags)
    assertTrue(details.isIndoor)
    assertEquals(warsaw, details.timeZone)
  }

  @Test
  fun `a phone microphone session is tagged as one`() = runTest {
    sessions.insert(session(deviceId = PhoneMicDeviceId, deviceModel = "Phone Microphone"))

    assertEquals(DataSource.PhoneMic, repo.observe("s1").first()?.source)
  }

  @Test
  fun `a stream carries now, avg, min and max over the whole session, rounded`() = runTest {
    sessions.insert(session())
    val pm25 = streams.findOrCreate("s1", "AirBeamMini-PM2.5", "µg/m³")
    measurements.insertAll(
      listOf(
        Measurement(pm25, t0, 3.0),
        Measurement(pm25, t0 + 1.seconds, 10.4),
        Measurement(pm25, t0 + 2.seconds, 6.0),
      ),
    )

    assertEquals(
      StationStream(
        id = pm25, sensorName = "AirBeamMini-PM2.5", unitSymbol = "µg/m³",
        value = 6, min = 3, max = 10, measuredAt = t0 + 2.seconds, avg = 6, // 19.4 / 3 = 6.47
      ),
      repo.observe("s1").first()?.streams?.single(),
    )
  }

  @Test
  fun `a stream with no readings yet has no values`() = runTest {
    sessions.insert(session())
    val pm25 = streams.findOrCreate("s1", "AirBeamMini-PM2.5", "µg/m³")

    assertEquals(
      StationStream(pm25, "AirBeamMini-PM2.5", "µg/m³", value = null, min = null, max = null, measuredAt = null),
      repo.observe("s1").first()?.streams?.single(),
    )
  }

  @Test
  fun `streams come in chip order, not insertion order`() = runTest {
    sessions.insert(session())
    streams.findOrCreate("s1", "AirBeamMini-F", "F")
    streams.findOrCreate("s1", "AirBeamMini-PM2.5", "µg/m³")
    streams.findOrCreate("s1", "AirBeamMini-PM1", "µg/m³")

    assertEquals(
      listOf("AirBeamMini-PM1", "AirBeamMini-PM2.5", "AirBeamMini-F"),
      repo.observe("s1").first()?.streams?.map { it.sensorName },
    )
  }

  @Test
  fun `an unknown session is null`() = runTest {
    assertNull(repo.observe("nope").first())
  }

  @Test
  fun `a new measurement re-emits`() = runTest {
    sessions.insert(session())
    val pm25 = streams.findOrCreate("s1", "AirBeamMini-PM2.5", "µg/m³")

    repo.observe("s1").test {
      assertNull(awaitItem()?.streams?.single()?.value)
      measurements.insertAll(listOf(Measurement(pm25, t0, 5.0)))
      assertEquals(5, awaitItem()?.streams?.single()?.value)
    }
  }

  @Test
  fun `points are one stream's readings, oldest first, on the session's wall clock`() = runTest {
    sessions.insert(session())
    val pm1 = streams.findOrCreate("s1", "AirBeamMini-PM1", "µg/m³")
    val pm25 = streams.findOrCreate("s1", "AirBeamMini-PM2.5", "µg/m³")
    measurements.insertAll(
      listOf(
        Measurement(pm25, t0 + 1.seconds, 7.0),
        Measurement(pm25, t0, 5.0),
        Measurement(pm1, t0, 99.0),
      ),
    )
    // The chart formats x as UTC, so x carries the +2 h CEST offset (same trick as the station graph).
    val x0 = (t0.epochSeconds + 2.hours.inWholeSeconds).toDouble()

    assertEquals(
      listOf(ChartPoint(x0, 5.0), ChartPoint(x0 + 1, 7.0)),
      repo.observePoints("s1", "AirBeamMini-PM2.5", warsaw).first(),
    )
  }
}