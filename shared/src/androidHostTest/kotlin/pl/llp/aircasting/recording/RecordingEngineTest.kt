package pl.llp.aircasting.recording

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FakeAirBeamLink
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.data.local.LocalMeasurementStreamRepository
import pl.llp.aircasting.data.local.LocalSessionRepository
import pl.llp.aircasting.data.local.testDatabase
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
  private val sessions = LocalSessionRepository(db.sessionQueries)
  private val streams = LocalMeasurementStreamRepository(db.measurementStreamQueries)
  private val link = FakeAirBeamLink()
  private val host = FakeBackgroundHost()
  private var time = Instant.parse("2026-10-20T10:00:00Z")
  private val clock = object : Clock {
    override fun now() = time
  }
  private val engine = RecordingEngine(link, sessions, streams, host, clock, TimeZone.of("Europe/Warsaw"))
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