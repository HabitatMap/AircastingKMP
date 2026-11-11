package pl.llp.aircasting.recording

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.data.local.LocalMeasurementRepository
import pl.llp.aircasting.data.local.LocalMeasurementStreamRepository
import pl.llp.aircasting.data.local.LocalSessionRepository
import pl.llp.aircasting.data.local.testDatabase
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.settings.mic.MicrophoneLevelSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class MicrophoneRecorderTest {
  private val db = testDatabase()
  private val sessions = LocalSessionRepository(db.sessionQueries, Dispatchers.Unconfined)
  private val streams = LocalMeasurementStreamRepository(db.measurementStreamQueries, Dispatchers.Unconfined)
  private val measurements =
    LocalMeasurementRepository(db.measurementQueries, db.pendingUploadQueries, Dispatchers.Unconfined)
  private val levels = FakeLevels()
  private val host = FakeHost()
  private var time = Instant.parse("2026-11-10T10:00:00Z")
  private val clock = object : Clock {
    override fun now() = time
  }
  private val krakow = GeoLocation(50.06, 19.94)
  private val recorder = MicrophoneRecorder(
    levels = levels,
    calibrationOffset = { 100 },
    sessions = sessions,
    streams = streams,
    measurements = measurements,
    location = StaticLocation(krakow),
    host = host,
    clock = clock,
    timeZone = TimeZone.of("Europe/Warsaw"),
  )
  private val details = NewSessionDetails(name = "Street noise", tags = listOf("traffic"))

  @Test
  fun start_stores_a_mobile_session_on_the_builtin_microphone() = runTest {
    assertTrue(recorder.start(details))

    val session = sessions.observeAll().first().single()
    assertEquals(SessionType.MOBILE, session.type)
    assertEquals(SessionStatus.RECORDING, session.status)
    assertEquals("Street noise", session.name)
    assertEquals(listOf("traffic"), session.tags)
    assertEquals("Builtin", session.deviceId) // legacy MicrophoneDeviceItem.DEFAULT_ID
    assertEquals("Phone Microphone", session.deviceModel)
    assertEquals(
      listOf(StreamSpec("Phone Microphone", "dB")),
      streams.findBySession(session.uuid).map { StreamSpec(it.sensorName, it.unitSymbol) },
    )
    assertTrue(host.running)
  }

  /** Legacy MicrophoneReader posts the first block of each new second, calibrated. */
  @Test
  fun keeps_the_first_calibrated_level_of_each_second() = runTest {
    backgroundScope.launch { recorder.run() }
    recorder.start(details)
    runCurrent()

    levelAt("2026-11-10T10:00:00.100Z", -50.0)
    levelAt("2026-11-10T10:00:00.600Z", -45.0) // same second: dropped
    levelAt("2026-11-10T10:00:01.050Z", -30.0)

    val stream = streams.findBySession(onlySessionUuid()).single()
    assertEquals(
      listOf(
        Measurement(stream.id, Instant.parse("2026-11-10T10:00:00.100Z"), 50.0, krakow),
        Measurement(stream.id, Instant.parse("2026-11-10T10:00:01.050Z"), 70.0, krakow),
      ),
      measurements.observeRecent(stream.id, 10).first().reversed(),
    )
  }

  /** Legacy: "You can't start 2 microphone sessions at once". */
  @Test
  fun a_second_session_is_refused_while_one_records() = runTest {
    assertTrue(recorder.start(details))

    assertFalse(recorder.start(details))
    assertEquals(1, sessions.observeAll().first().size)
  }

  @Test
  fun finish_ends_the_session_and_releases_the_microphone() = runTest {
    backgroundScope.launch { recorder.run() }
    recorder.start(details)
    runCurrent()
    val uuid = onlySessionUuid()
    time = Instant.parse("2026-11-10T10:05:00Z")

    assertTrue(recorder.finish(uuid))
    runCurrent()

    assertEquals(time, sessions.find(uuid)?.endTime)
    assertEquals(0, levels.flow.subscriptionCount.value)
    assertFalse(host.running)
  }

  /** The Active tab finishes every session through one call; an AirBeam one isn't ours. */
  @Test
  fun finish_of_another_session_is_left_to_the_caller() = runTest {
    recorder.start(details)

    assertFalse(recorder.finish("some-airbeam-session"))
    assertTrue(host.running)
  }

  private suspend fun onlySessionUuid() = sessions.observeAll().first().single().uuid

  private suspend fun TestScope.levelAt(at: String, dbFs: Double) {
    time = Instant.parse(at)
    levels.flow.emit(dbFs)
    runCurrent()
  }
}

private class FakeLevels : MicrophoneLevelSource {
  val flow = MutableSharedFlow<Double>()
  override fun levels() = flow
}

private class FakeHost : BackgroundSessionHost {
  var running = false
  override fun start() { running = true }
  override fun stop() { running = false }
}

private class StaticLocation(private val fix: GeoLocation) : PhoneLocation {
  override fun latest() = fix
  override suspend fun track() = awaitCancellation()
  override suspend fun awaitFix() = fix
}