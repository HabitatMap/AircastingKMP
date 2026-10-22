package pl.llp.aircasting.bluetooth.ui.scan

import app.cash.turbine.test
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConnectResult
import pl.llp.aircasting.bluetooth.DefaultAirBeamLink
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.Transport
import pl.llp.aircasting.bluetooth.transport.FakeConnection
import pl.llp.aircasting.bluetooth.transport.FakeConnector
import pl.llp.aircasting.bluetooth.transport.connected
import pl.llp.aircasting.scan.ConnectionUiState
import pl.llp.aircasting.scan.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.scan.SCAN_WINDOW
import pl.llp.aircasting.scan.ScanUiState
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
    val link = DefaultAirBeamLink(connector, backgroundScope)
    val vm = ScanViewModel(connector, link)
    assertEquals(ConnectionUiState.None, vm.connection.value)
  }

  @Test
  fun onConnectClicked_routes_the_target_to_the_connector() = runTest {
    val connector = FakeConnector(setOf(Transport.BLE))
    val link = DefaultAirBeamLink(connector, backgroundScope)
    val vm = ScanViewModel(connector, link)

    vm.onConnectClicked(ab3)
    testScheduler.advanceUntilIdle()

    assertEquals(ab3, connector.connectedTarget)
  }

  @Test
  fun onConnectClicked_emits_Connecting_then_Connected_on_Ready() = runTest {
    val connector = FakeConnector(setOf(Transport.BLE))
    val link = DefaultAirBeamLink(connector, backgroundScope)
    val vm = ScanViewModel(connector, link)

    vm.connection.test {
      assertEquals(ConnectionUiState.None, awaitItem())
      vm.onConnectClicked(ab3)
      assertEquals(ConnectionUiState.Connecting, awaitItem())

      connector.willConnect(FakeConnection().connected())
      assertEquals(
        ConnectionUiState.Connected(AirBeamDevice.AirBeam3),
        awaitItem(),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun onConnectClicked_emits_Failed_when_connection_reports_Failure() = runTest {
    val connector = FakeConnector(setOf(Transport.BLE))
    val link = DefaultAirBeamLink(connector, backgroundScope)
    val vm = ScanViewModel(connector, link)

    vm.connection.test {
      assertEquals(ConnectionUiState.None, awaitItem())
      vm.onConnectClicked(ab3)
      assertEquals(ConnectionUiState.Connecting, awaitItem())

      connector.willConnect(ConnectResult.Failed(FailureReason.HandshakeFailed))
      assertEquals(ConnectionUiState.Failed(FailureReason.HandshakeFailed), awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }
  private fun TestScope.scanVm(scan: kotlinx.coroutines.flow.Flow<List<DiscoveredAirBeam>>): ScanViewModel {
    val connector = FakeConnector(setOf(Transport.BLE), scanFlow = scan)
    val vm = ScanViewModel(connector, DefaultAirBeamLink(connector, backgroundScope))
    backgroundScope.launch { vm.state.collect {} } // a subscriber is what starts the scan
    return vm
  }

  @Test
  fun scan_lists_devices_then_finishes_when_the_window_closes() = runTest {
    val vm = scanVm(flow { emit(listOf(ab3)); awaitCancellation() })
    runCurrent()

    assertEquals(ScanUiState.Scanning(listOf(ab3)), vm.state.value)

    advanceTimeBy(SCAN_WINDOW)
    runCurrent()
    assertEquals(ScanUiState.Finished(listOf(ab3)), vm.state.value)
  }

  @Test
  fun scan_finishes_empty_when_nothing_is_found() = runTest {
    val vm = scanVm(flow { emit(emptyList()); awaitCancellation() })

    advanceTimeBy(SCAN_WINDOW)
    runCurrent()
    assertEquals(ScanUiState.Finished(emptyList()), vm.state.value)
  }

  @Test
  fun rescan_after_a_failure_starts_a_fresh_scan() = runTest {
    var attempts = 0
    val vm = scanVm(
      flow {
        if (attempts++ == 0) error("radio off")
        emit(listOf(ab3))
        awaitCancellation()
      },
    )
    runCurrent()
    assertEquals(ScanUiState.Error(FailureReason.RadioOrPermissionMissing), vm.state.value)

    vm.rescan()
    runCurrent()
    assertEquals(ScanUiState.Scanning(listOf(ab3)), vm.state.value)
  }
}
