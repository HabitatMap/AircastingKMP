package com.lunarlogic.aircasting.bluetooth.ui.scan

import app.cash.turbine.test
import com.lunarlogic.aircasting.bluetooth.AirBeamDevice
import com.lunarlogic.aircasting.bluetooth.ConnectionStatus
import com.lunarlogic.aircasting.bluetooth.DeviceId
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.FailureReason
import com.lunarlogic.aircasting.bluetooth.Transport
import com.lunarlogic.aircasting.bluetooth.transport.ControllableConnection
import com.lunarlogic.aircasting.bluetooth.transport.FakeConnector
import com.lunarlogic.aircasting.ui.scan.ConnectionUiState
import com.lunarlogic.aircasting.ui.scan.ScanViewModel
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
    val vm = ScanViewModel(FakeConnector(setOf(Transport.BLE)))
    assertEquals(ConnectionUiState.None, vm.connection.value)
  }

  @Test
  fun onConnectClicked_routes_the_target_to_the_connector() = runTest {
    val connector = FakeConnector(setOf(Transport.BLE))
    val vm = ScanViewModel(connector)

    vm.onConnectClicked(ab3)
    testScheduler.advanceUntilIdle()

    assertEquals(ab3, connector.connectedTarget)
  }

  @Test
  fun onConnectClicked_emits_Connecting_then_Connected_on_Ready() = runTest {
    val conn = ControllableConnection(
      status = MutableStateFlow(ConnectionStatus.Ready(AirBeamDevice.AirBeam3)),
    )
    val vm = ScanViewModel(FakeConnector(setOf(Transport.BLE), connection = conn))

    vm.connection.test {
      assertEquals(ConnectionUiState.None, awaitItem())
      vm.onConnectClicked(ab3)
      assertEquals(ConnectionUiState.Connecting, awaitItem())
      assertEquals(
        ConnectionUiState.Connected(AirBeamDevice.AirBeam3),
        awaitItem(),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun onConnectClicked_emits_Failed_when_connection_reports_Failure() = runTest {
    val conn = ControllableConnection(
      status = MutableStateFlow(ConnectionStatus.Failed(FailureReason.HandshakeFailed)),
    )
    val vm = ScanViewModel(FakeConnector(setOf(Transport.BLE), connection = conn))

    vm.connection.test {
      assertEquals(ConnectionUiState.None, awaitItem())
      vm.onConnectClicked(ab3)
      assertEquals(ConnectionUiState.Connecting, awaitItem())
      assertEquals(ConnectionUiState.Failed(FailureReason.HandshakeFailed), awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }
}
