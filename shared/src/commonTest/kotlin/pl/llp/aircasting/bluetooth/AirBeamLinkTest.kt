package pl.llp.aircasting.bluetooth

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.bluetooth.transport.FakeConnection
import pl.llp.aircasting.bluetooth.transport.FakeConnector
import pl.llp.aircasting.bluetooth.transport.connected
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

/**
 * Virtual time: the link runs in `backgroundScope`, so `advanceTimeBy` drives the reconnect
 * interval. Reconnect rules are legacy's (`AirBeamService` + `AirBeamReconnector`): mobile
 * sessions only, every 2 s, no attempt cap, resume — don't reconfigure — on success.
 */
class AirBeamLinkTest {

  private val target = DiscoveredAirBeam(DeviceId("00:11:22:33:44:55"), "AirBeam3-1234", AirBeamDevice.AirBeam3)
  private val other = DiscoveredAirBeam(DeviceId("66:77:88:99:AA:BB"), "AirBeam3-5678", AirBeamDevice.AirBeam3)
  private val mobile = SessionConfig.Mobile(Uuid.random())
  private val fixed = SessionConfig.FixedCellular(Uuid.random(), "token", 1.0, 2.0)
  private val connector = FakeConnector(setOf(Transport.BLE))

  private fun TestScope.link() = DefaultAirBeamLink(connector, backgroundScope, reconnectInterval = 2.seconds)

  @Test
  fun starts_idle() = runTest {
    assertEquals(LinkState.Idle, link().state.value)
  }

  @Test
  fun connect_settles_on_Connected() = runTest {
    val link = link()
    connector.willConnect(FakeConnection().connected())

    assertEquals(LinkState.Connected(target), link.connect(target))
    assertEquals(LinkState.Connected(target), link.state.value)
  }

  @Test
  fun connect_failure_keeps_its_reason() = runTest {
    val link = link()
    connector.willConnect(ConnectResult.Failed(FailureReason.NoDeviceFound))

    assertEquals(LinkState.Failed(FailureReason.NoDeviceFound), link.connect(target))
  }

  @Test
  fun disconnect_releases_the_connection_and_goes_idle() = runTest {
    val link = link()
    val connection = FakeConnection()
    connector.willConnect(connection.connected())
    link.connect(target)

    link.disconnect()

    assertEquals(LinkState.Idle, link.state.value)
    assertEquals(1, connection.disconnects)
  }

  /** The user backs out while the radio is still trying. */
  @Test
  fun disconnect_while_connecting_cancels_the_attempt() = runTest {
    val link = link()
    var settled: LinkState? = null
    launch { settled = link.connect(target) } // the connector never answers
    runCurrent()
    assertEquals(LinkState.Connecting(target), link.state.value)

    link.disconnect()
    runCurrent()

    assertEquals(LinkState.Idle, link.state.value)
    assertEquals(LinkState.Idle, settled)
  }

  /** The connection outlives the screen that asked for it. */
  @Test
  fun a_cancelled_caller_does_not_cancel_the_connect() = runTest {
    val link = link()
    val caller = launch { link.connect(target) }
    runCurrent()
    caller.cancel()

    connector.willConnect(FakeConnection().connected())
    runCurrent()

    assertEquals(LinkState.Connected(target), link.state.value)
  }

  @Test
  fun connecting_to_another_device_releases_the_first() = runTest {
    val link = link()
    val first = FakeConnection()
    connector.willConnect(first.connected(), FakeConnection().connected())
    link.connect(target)

    link.connect(other)

    assertEquals(1, first.disconnects)
    assertEquals(LinkState.Connected(other), link.state.value)
  }

  @Test
  fun configure_needs_a_ready_link() = runTest {
    assertEquals(ConfigResult.NotConnected, link().configure(mobile))
  }

  /** The specific failure reaches the caller — the UI can say "wrong Wi-Fi password". */
  @Test
  fun configure_hands_back_the_device_result() = runTest {
    val link = link()
    val connection = FakeConnection(configureResult = ConfigResult.WifiPassFailure)
    connector.willConnect(connection.connected())
    link.connect(target)

    assertEquals(ConfigResult.WifiPassFailure, link.configure(mobile))
    assertEquals(listOf<SessionConfig>(mobile), connection.configured)
  }

  @Test
  fun a_drop_with_no_session_is_ConnectionLost() = runTest {
    val link = link()
    val connection = FakeConnection()
    connector.willConnect(connection.connected())
    link.connect(target)

    connection.drop()
    advance(10.seconds)

    assertEquals(LinkState.Failed(FailureReason.ConnectionLost), link.state.value)
    assertEquals(1, connector.targets.size)
    assertEquals(1, connection.disconnects)
  }

  /** Fixed sessions record on the device itself; the phone has nothing to reconnect for. */
  @Test
  fun a_drop_during_a_fixed_session_does_not_reconnect() = runTest {
    val link = link()
    val connection = FakeConnection()
    connector.willConnect(connection.connected())
    link.connect(target)
    link.configure(fixed)

    connection.drop()
    advance(10.seconds)

    assertEquals(LinkState.Failed(FailureReason.ConnectionLost), link.state.value)
    assertEquals(1, connector.targets.size)
  }

  @Test
  fun a_failed_mobile_configure_arms_no_reconnect() = runTest {
    val link = link()
    val connection = FakeConnection(configureResult = ConfigResult.UnknownFailure)
    connector.willConnect(connection.connected())
    link.connect(target)
    link.configure(mobile)

    connection.drop()
    advance(10.seconds)

    assertEquals(LinkState.Failed(FailureReason.ConnectionLost), link.state.value)
  }

  @Test
  fun a_drop_during_a_mobile_session_reconnects_every_interval() = runTest {
    val link = link()
    val dropped = FakeConnection()
    connector.willConnect(dropped.connected())
    link.connect(target)
    link.configure(mobile)

    dropped.drop()
    runCurrent()
    assertEquals(LinkState.Reconnecting(target, attempt = 1), link.state.value)
    assertEquals(1, dropped.disconnects) // released before trying again

    advance(1.seconds)
    assertEquals(1, connector.targets.size) // still waiting out the interval

    advance(1.seconds)
    assertEquals(listOf(target, target), connector.targets)

    connector.willConnect(ConnectResult.Failed(FailureReason.NoDeviceFound))
    runCurrent()
    assertEquals(LinkState.Reconnecting(target, attempt = 2), link.state.value)
  }

  /** On success the device is told to carry on — not handed a new session config. */
  @Test
  fun a_reconnect_resumes_the_mobile_session() = runTest {
    val link = link()
    val dropped = FakeConnection()
    val fresh = FakeConnection()
    connector.willConnect(dropped.connected(), fresh.connected())
    link.connect(target)
    link.configure(mobile)

    dropped.drop()
    advance(2.seconds)

    assertEquals(LinkState.Connected(target), link.state.value)
    assertEquals(listOf(mobile), fresh.resumed)
    assertEquals(emptyList(), fresh.configured)
  }

  @Test
  fun reconnect_never_gives_up_on_its_own() = runTest {
    val link = link()
    val dropped = FakeConnection()
    connector.willConnect(dropped.connected())
    link.connect(target)
    link.configure(mobile)
    repeat(50) { connector.willConnect(ConnectResult.Failed(FailureReason.LinkTimeout)) }

    dropped.drop()
    advance(100.seconds)

    assertEquals(LinkState.Reconnecting(target, attempt = 51), link.state.value)
  }

  @Test
  fun a_failed_resume_counts_as_a_failed_attempt() = runTest {
    val link = link()
    val dropped = FakeConnection()
    val refusing = FakeConnection(resumeResult = ConfigResult.UnknownFailure)
    connector.willConnect(dropped.connected(), refusing.connected())
    link.connect(target)
    link.configure(mobile)

    dropped.drop()
    advance(2.seconds)

    assertEquals(LinkState.Reconnecting(target, attempt = 2), link.state.value)
    assertEquals(1, refusing.disconnects)
  }

  /** Standalone mode and stop both end the link; the session-level difference is not ours. */
  @Test
  fun disconnect_stops_a_reconnect() = runTest {
    val link = link()
    val dropped = FakeConnection()
    connector.willConnect(dropped.connected())
    link.connect(target)
    link.configure(mobile)
    dropped.drop()
    runCurrent()

    link.disconnect()
    advance(60.seconds)

    assertEquals(LinkState.Idle, link.state.value)
    assertEquals(1, connector.targets.size)
  }

  @Test
  fun readings_carry_on_across_a_reconnect() = runTest {
    val link = link()
    val dropped = FakeConnection()
    val fresh = FakeConnection()
    connector.willConnect(dropped.connected(), fresh.connected())
    val received = mutableListOf<SensorReading>()
    backgroundScope.launch { link.readings.collect { received += it } }
    link.connect(target)
    link.configure(mobile)

    dropped.send(reading(1.0))
    dropped.drop()
    advance(2.seconds)
    fresh.send(reading(2.0))
    runCurrent()

    assertEquals(listOf(reading(1.0), reading(2.0)), received)
  }

  private fun reading(value: Double) = SensorReading("AirBeam3-PM2.5", "µg/m³", value, measuredAt = null)

  private fun TestScope.advance(by: Duration) {
    advanceTimeBy(by)
    runCurrent()
  }
}
