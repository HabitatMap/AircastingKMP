package com.lunarlogic.aircasting.bluetooth.transport

import app.cash.turbine.test
import com.lunarlogic.aircasting.bluetooth.AirBeamDevice
import com.lunarlogic.aircasting.bluetooth.DeviceId
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.Transport
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

private val ab3 = DiscoveredAirBeam(DeviceId("ble-1"), "AirBeam3", AirBeamDevice.AirBeam3)
private val ab2 = DiscoveredAirBeam(DeviceId("mac-1"), "AirBeam2", AirBeamDevice.AirBeam2)

class CompositeAirBeamConnectorTest {

  @Test
  fun supported_transports_is_the_union_of_delegates() {
    val composite = CompositeAirBeamConnector(
      listOf(
        FakeConnector(setOf(Transport.BLE)),
        FakeConnector(setOf(Transport.CLASSIC_SERIAL)),
      )
    )
    assertEquals(setOf(Transport.BLE, Transport.CLASSIC_SERIAL), composite.supportedTransports)
  }

  @Test
  fun scan_merges_results_from_all_delegates() = runTest {
    val composite = CompositeAirBeamConnector(
      listOf(
        FakeConnector(setOf(Transport.BLE), flowOf(listOf(ab3))),
        FakeConnector(setOf(Transport.CLASSIC_SERIAL), flowOf(listOf(ab2))),
      )
    )
    composite.scan().test {
      // combine emits once both delegates have produced a value; flatten joins them.
      assertContentEquals(listOf(ab3, ab2), awaitItem())
      awaitComplete()
    }
  }

  @Test
  fun connect_routes_to_the_delegate_that_supports_the_transport() = runTest {
    val ble = FakeConnector(setOf(Transport.BLE))
    val classic = FakeConnector(setOf(Transport.CLASSIC_SERIAL))
    val composite = CompositeAirBeamConnector(listOf(ble, classic))

    val connection = composite.connect(ab2) // CLASSIC_SERIAL

    assertSame(FakeConnection, connection)
    assertEquals(ab2, classic.connectedTarget) // classic was used
    assertEquals(null, ble.connectedTarget)     // ble was not touched
  }

  @Test
  fun connect_fails_when_no_delegate_supports_the_transport() = runTest {
    val composite = CompositeAirBeamConnector(
      listOf(FakeConnector(setOf(Transport.BLE)))
    )
    assertFailsWith<IllegalStateException> {
      composite.connect(ab2) // CLASSIC_SERIAL — unsupported
    }
  }

  @Test
  fun scan_stalls_when_any_delegate_never_emits() = runTest {
    // Documents the combine contract: no output until EVERY source has emitted once.
    val composite = CompositeAirBeamConnector(
      listOf(
        FakeConnector(setOf(Transport.BLE), flowOf(listOf(ab3))),
        FakeConnector(setOf(Transport.CLASSIC_SERIAL), emptyFlow()),
      )
    )
    composite.scan().test {
      awaitComplete() // completes with zero items — the ab3 result never surfaces
    }
  }
}
