package pl.llp.aircasting.recording

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.FakeAirBeamLink
import pl.llp.aircasting.bluetooth.LinkState
import pl.llp.aircasting.bluetooth.SensorReading
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.data.local.LocalMeasurementRepository
import pl.llp.aircasting.data.local.LocalMeasurementStreamRepository
import pl.llp.aircasting.data.local.LocalSessionRepository
import pl.llp.aircasting.data.local.LocalUploadOutbox
import pl.llp.aircasting.data.local.testDatabase
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Measurement
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionStatus
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RecordingEngineTest {

  private val db = testDatabase()
  // Unconfined: repository writes run inline, so `runCurrent()` alone settles the collector.
  private val sessions = LocalSessionRepository(db.sessionQueries, Dispatchers.Unconfined)
  private val streams = LocalMeasurementStreamRepository(db.measurementStreamQueries, Dispatchers.Unconfined)
  private val measurements =
    LocalMeasurementRepository(
      db.measurementQueries,
      db.pendingUploadQueries,
      Dispatchers.Unconfined
    )
  private val outbox = LocalUploadOutbox(db.pendingUploadQueries, Dispatchers.Unconfined)
  private val link = FakeAirBeamLink()
  private val host = FakeBackgroundHost()
  private var phone: GeoLocation? = GeoLocation(50.06, 19.94)
  private var time = Instant.parse("2026-10-20T10:00:00Z")
  private val clock = object : Clock {
    override fun now() = time
  }
  private val engine = RecordingEngine(
    link, sessions, streams, measurements, { phone }, host, clock, TimeZone.of("Europe/Warsaw"),
  )
  private val miniV2 = DiscoveredAirBeam(DeviceId("AA:BB:CC:DD:EE:FF"), "AirBeamMini-1234", AirBeamDevice.Mini.V2)
  private val details = NewSessionDetails(name = "Morning walk", tags = listOf("bike", "commute"))

  @Test
  fun start_configures_a_mobile_session_and_records_it() = runTest {
    assertEquals(ConfigResult.Success, engine.start(miniV2, details))

    val uuid = (link.configured.single() as SessionConfig.Mobile).uuid.toString()
    assertEquals(
      Session(
        uuid = uuid,
        type = SessionType.MOBILE,
        status = SessionStatus.RECORDING,
        name = "Morning walk",
        tags = listOf("bike", "commute"),
        startTime = time,
        timeZone = "Europe/Warsaw",
        measurementInterval = 1.seconds,
        deviceId = "AA:BB:CC:DD:EE:FF",
        deviceModel = "AirBeamMini",
      ),
      sessions.find(uuid),
    )
  }
  /** v3 can't add a stream later, so all of them exist before the first upload. */
  @Test
  fun start_creates_every_stream_of_the_device() = runTest {
    engine.start(miniV2, details)

    val uuid = link.configured.single().uuid.toString()
    assertEquals(
      streamsFor(AirBeamDevice.Mini.V2).toSet(),
      streams.findBySession(uuid).map { StreamSpec(it.sensorName, it.unitSymbol) }.toSet(),
    )
  }

  @Test
  fun start_keeps_the_app_alive_in_the_background() = runTest {
    engine.start(miniV2, details)

    assertTrue(host.running)
  }
  /** The record screen shows the specific reason and keeps the details for a retry. */
  @Test
  fun a_rejected_configure_writes_nothing() = runTest {
    link.configureResult = ConfigResult.NotSyncedFailure

    assertEquals(ConfigResult.NotSyncedFailure, engine.start(miniV2, details))
    assertEquals(emptyList(), sessions.observeAll().first())
    assertFalse(host.running)
  }

  /** A session with no streams could never be uploaded. */
  @Test
  fun a_device_with_no_catalogued_streams_is_never_configured() = runTest {
    val ab3 = DiscoveredAirBeam(DeviceId("11:22:33:44:55:66"), "AirBeam3-1234", AirBeamDevice.AirBeam3)

    assertEquals(ConfigResult.BadConfigFailure, engine.start(ab3, details))
    assertEquals(emptyList(), link.configured)
    assertEquals(emptyList(), sessions.observeAll().first())
  }
  /** Stop is all the upload loop needs: it drains the outbox and calls /finish on its own. */
  @Test
  fun stop_finishes_the_session_for_upload() = runTest {
    engine.start(miniV2, details)
    val uuid = link.configured.single().uuid.toString()
    time += 5.minutes

    engine.stop()

    val stopped = sessions.find(uuid)!!
    assertEquals(SessionStatus.FINISHED, stopped.status)
    assertEquals(time, stopped.endTime)
    assertEquals(listOf(uuid), sessions.observeAwaitingFinish().first())
    assertEquals(1, link.disconnects)
    assertFalse(host.running)
  }
  /** The AirBeam keeps recording on its own; the session stays open for a later sync. */
  @Test
  fun standalone_disconnects_but_keeps_the_session_open() = runTest {
    engine.start(miniV2, details)
    val uuid = link.configured.single().uuid.toString()

    engine.goStandalone()

    val standalone = sessions.find(uuid)!!
    assertEquals(SessionStatus.DISCONNECTED, standalone.status)
    assertNull(standalone.endTime)
    assertEquals(emptyList(), sessions.observeAwaitingFinish().first())
    assertEquals(1, link.disconnects)
    assertFalse(host.running)
  }

  @Test
  fun a_reading_during_a_session_is_stored_and_queued_for_upload() = runTest {
    val uuid = recording()
    val at = Instant.parse("2026-10-20T10:00:05Z")

    link.send(pm25(12.0, at))
    runCurrent()

    assertEquals(
      listOf(Measurement(streamId(uuid, "AirBeamMini-PM2.5"), at, 12.0, GeoLocation(50.06, 19.94))),
      outbox.pending(uuid, limit = 10),
    )
  }
  /** V1 devices send no time; the phone clock at receipt stands in. */
  @Test
  fun a_reading_without_device_time_takes_the_phone_clock() = runTest {
    val uuid = recording()

    link.send(pm25(12.0, at = null))
    runCurrent()

    assertEquals(listOf(time), outbox.pending(uuid, limit = 10).map { it.time })
  }
  /** v3 mobile frames have no "no location" value: before the first fix, legacy's default. */
  @Test
  fun no_phone_fix_yet_stamps_the_default_location() = runTest {
    phone = null
    val uuid = recording()

    link.send(pm25(12.0, time))
    runCurrent()

    assertEquals(listOf(GeoLocation(40.7128, -74.0060)), outbox.pending(uuid, limit = 10).map { it.location })
  }
  /** A V2 backlog replays what may already be stored; the (stream, time) key absorbs it. */
  @Test
  fun a_repeated_reading_is_stored_and_queued_once() = runTest {
    val uuid = recording()

    link.send(pm25(12.0, time))
    link.send(pm25(12.0, time))
    runCurrent()

    assertEquals(1, outbox.pending(uuid, limit = 10).size)
  }

  @Test
  fun readings_outside_a_session_are_dropped() = runTest {
    running()
    link.send(pm25(1.0, time))       // before start
    runCurrent()
    val uuid = recording(alreadyRunning = true)
    engine.stop()
    link.send(pm25(2.0, time + 1.seconds)) // after stop
    runCurrent()

    assertEquals(emptyList(), outbox.pending(uuid, limit = 10))
  }

  /** It has no stream — and v3 could never take it — so it's dropped, not crashed on. */
  @Test
  fun a_reading_from_an_uncatalogued_sensor_is_dropped() = runTest {
    val uuid = recording()

    link.send(SensorReading("AirBeam3-PM10", "µg/m³", 5.0, time))
    link.send(pm25(12.0, time))
    runCurrent()

    assertEquals(listOf(12.0), outbox.pending(uuid, limit = 10).map { it.value })
  }

  @Test
  fun the_session_follows_the_link_through_a_reconnect() = runTest {
    val uuid = recording()

    link.state.value = LinkState.Reconnecting(miniV2, attempt = 1)
    runCurrent()
    assertEquals(SessionStatus.DISCONNECTED, sessions.find(uuid)!!.status)

    link.state.value = LinkState.Connected(miniV2)
    runCurrent()
    assertEquals(SessionStatus.RECORDING, sessions.find(uuid)!!.status)
  }

  /** Nothing more will come over this link; the user finishes the session from the UI. */
  @Test
  fun a_failed_link_leaves_the_session_disconnected_not_finished() = runTest {
    val uuid = recording()

    link.state.value = LinkState.Failed(FailureReason.ConnectionLost)
    runCurrent()

    val session = sessions.find(uuid)!!
    assertEquals(SessionStatus.DISCONNECTED, session.status)
    assertNull(session.endTime)
  }

  // --- helpers ---

  private fun TestScope.running() {
    backgroundScope.launch { engine.run() }
    runCurrent()
  }

  /** A Mini V2 session in progress, the engine collecting; returns its uuid. */
  private suspend fun TestScope.recording(alreadyRunning: Boolean = false): String {
    if (!alreadyRunning) running()
    link.state.value = LinkState.Connected(miniV2)
    engine.start(miniV2, details)
    runCurrent()
    return link.configured.last().uuid.toString()
  }
  private suspend fun streamId(uuid: String, sensor: String) =
    streams.findBySession(uuid).single { it.sensorName == sensor }.id

  private fun pm25(value: Double, at: Instant?) = SensorReading("AirBeamMini-PM2.5", "µg/m³", value, at)

}

private class FakeBackgroundHost : BackgroundSessionHost {
  var running = false
    private set

  override fun start() {
    running = true
  }

  override fun stop() {
    running = false
  }
}