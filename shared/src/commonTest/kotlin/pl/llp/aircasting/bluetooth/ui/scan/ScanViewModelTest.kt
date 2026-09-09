package pl.llp.aircasting.bluetooth.ui.scan

import app.cash.turbine.test
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConnectionStatus
import pl.llp.aircasting.bluetooth.DefaultAirBeamSessionController
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.Transport
import pl.llp.aircasting.bluetooth.transport.ControllableConnection
import pl.llp.aircasting.bluetooth.transport.FakeConnector
import pl.llp.aircasting.scan.ConnectionUiState
import pl.llp.aircasting.scan.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val ab3 = DiscoveredAirBeam(DeviceId("id-ab3"), "airbeam3", AirBeamDevice.AirBeam3)

  @Test
  fun connection_state_starts_as_None() = runTest {
    val connector = FakeConnector(setOf(Transport.BLE))
    val controller = DefaultAirBeamSessionController(connector)
    val vm = ScanViewModel(connector, controller)
    assertEquals(ConnectionUiState.None, vm.connection.value)
  }

  @Test
  fun onConnectClicked_routes_the_target_to_the_connector() = runTest {
    val connector = FakeConnector(setOf(Transport.BLE))
    val controller = DefaultAirBeamSessionController(connector)
    val vm = ScanViewModel(connector, controller)

    vm.onConnectClicked(ab3)
    testScheduler.advanceUntilIdle()

    assertEquals(ab3, connector.connectedTarget)
  }

  @Test
  fun onConnectClicked_emits_Connecting_then_Connected_on_Ready() = runTest {
    val connStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Connecting)
    val conn = ControllableConnection(status = connStatus)
    val connector = FakeConnector(setOf(Transport.BLE), connection = conn)
    val controller = DefaultAirBeamSessionController(connector)
    val vm = ScanViewModel(connector, controller)

    vm.connection.test {
      assertEquals(ConnectionUiState.None, awaitItem())
      vm.onConnectClicked(ab3)
      assertEquals(ConnectionUiState.Connecting, awaitItem())

      connStatus.value = ConnectionStatus.Ready(AirBeamDevice.AirBeam3)
      assertEquals(
        ConnectionUiState.Connected(AirBeamDevice.AirBeam3),
        awaitItem(),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun onConnectClicked_emits_Failed_when_connection_reports_Failure() = runTest {
    val connStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Connecting)
    val conn = ControllableConnection(status = connStatus)
    val connector = FakeConnector(setOf(Transport.BLE), connection = conn)
    val controller = DefaultAirBeamSessionController(connector)
    val vm = ScanViewModel(connector, controller)

    vm.connection.test {
      assertEquals(ConnectionUiState.None, awaitItem())
      vm.onConnectClicked(ab3)
      assertEquals(ConnectionUiState.Connecting, awaitItem())

      connStatus.value = ConnectionStatus.Failed(FailureReason.HandshakeFailed)
      assertEquals(ConnectionUiState.Failed(FailureReason.HandshakeFailed), awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }
}
