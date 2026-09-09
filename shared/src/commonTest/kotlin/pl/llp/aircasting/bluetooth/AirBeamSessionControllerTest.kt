package pl.llp.aircasting.bluetooth

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.llp.aircasting.bluetooth.transport.ControllableConnection
import pl.llp.aircasting.bluetooth.transport.FakeConnector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class AirBeamSessionControllerTest {

  private val target = DiscoveredAirBeam(
    id = DeviceId("00:11:22:33:44:55"),
    name = "AirBeam3-1234",
    device = AirBeamDevice.AirBeam3
  )

  @Test
  fun initialStateIsIdle() = runTest {
    val statusFlow = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connection = ControllableConnection(statusFlow)
    val connector = FakeConnector(setOf(Transport.BLE), connection = connection)
    val testDispatcher = UnconfinedTestDispatcher(testScheduler)
    val testScope = TestScope(testDispatcher)

    val controller = DefaultAirBeamSessionController(connector, externalScope = testScope)

    assertEquals(SessionState.Idle, controller.state.value)
  }

  @Test
  fun connectActionTransitionsToConnectingAndThenConnected() = runTest {
    val statusFlow = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Ready(AirBeamDevice.AirBeam3))
    val connection = ControllableConnection(statusFlow)
    val connector = FakeConnector(setOf(Transport.BLE), connection = connection)
    val testDispatcher = UnconfinedTestDispatcher(testScheduler)
    val testScope = TestScope(testDispatcher)

    val controller = DefaultAirBeamSessionController(connector, externalScope = testScope)

    controller.state.test {
      assertEquals(SessionState.Idle, awaitItem())

      controller.dispatch(SessionAction.Connect(target))

      val connectingState = awaitItem()
      assertTrue(connectingState is SessionState.Connecting)

      val connectedState = awaitItem()
      assertTrue(connectedState is SessionState.Connected)
      assertEquals(target, connectedState.target)
    }
  }

  @Test
  fun configureAndStartTransitionsToRecording() = runTest {
    val statusFlow = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Ready(AirBeamDevice.AirBeam3))
    val connection = ControllableConnection(statusFlow)
    val connector = FakeConnector(setOf(Transport.BLE), connection = connection)
    val testDispatcher = UnconfinedTestDispatcher(testScheduler)
    val testScope = TestScope(testDispatcher)

    val controller = DefaultAirBeamSessionController(connector, externalScope = testScope)

    controller.dispatch(SessionAction.Connect(target))

    val sessionConfig = SessionConfig.Mobile(uuid = Uuid.random())

    controller.state.test {
      val current = awaitItem()
      assertTrue(current is SessionState.Connected)

      controller.dispatch(SessionAction.ConfigureAndStart(sessionConfig))

      val configuringState = awaitItem()
      assertTrue(configuringState is SessionState.Configuring)

      val recordingState = awaitItem()
      assertTrue(recordingState is SessionState.Recording)
      assertEquals(sessionConfig, (recordingState as SessionState.Recording).config)
    }
  }

  @Test
  fun disconnectActionTransitionsToIdleAndDisconnects() = runTest {
    val statusFlow = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Ready(AirBeamDevice.AirBeam3))
    val connection = ControllableConnection(statusFlow)
    val connector = FakeConnector(setOf(Transport.BLE), connection = connection)
    val testDispatcher = UnconfinedTestDispatcher(testScheduler)
    val testScope = TestScope(testDispatcher)

    val controller = DefaultAirBeamSessionController(connector, externalScope = testScope)

    controller.dispatch(SessionAction.Connect(target))
    assertTrue(controller.state.value is SessionState.Connected)

    controller.dispatch(SessionAction.Disconnect)

    assertEquals(SessionState.Idle, controller.state.value)
    assertTrue(connection.disconnectCalled)
  }

  @Test
  fun unexpectedDisconnectionTriggersReconnecting() = runTest {
    val statusFlow = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Ready(AirBeamDevice.AirBeam3))
    val connection = ControllableConnection(statusFlow)
    val connector = FakeConnector(setOf(Transport.BLE), connection = connection)
    val testDispatcher = UnconfinedTestDispatcher(testScheduler)
    val testScope = TestScope(testDispatcher)

    val controller = DefaultAirBeamSessionController(connector, externalScope = testScope)

    controller.dispatch(SessionAction.Connect(target))
    assertTrue(controller.state.value is SessionState.Connected)

    statusFlow.value = ConnectionStatus.DisconnectedUnexpectedly

    val state = controller.state.value
    assertTrue(state is SessionState.Reconnecting)
    assertEquals(target, state.target)
  }
}
