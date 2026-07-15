package com.lunarlogic.aircasting.bluetooth.detection

import app.cash.turbine.test
import com.lunarlogic.aircasting.bluetooth.AirBeamDevice
import com.lunarlogic.aircasting.bluetooth.DeviceId
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.transport.accumulateDistinct
import com.lunarlogic.aircasting.bluetooth.v2_firmware_specific.MINI_V2_SERVICE
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AirBeamDetectionTest {
  @Test
  fun v2_uuid_present_wins_over_name() {
    assertEquals(
      AirBeamDevice.Mini.V2,
      airBeamFrom("airbeammini", listOf(MINI_V2_SERVICE))
    )
  }

  @Test
  fun mini_name_without_v2_uuid_is_v1() {
    assertEquals(
      AirBeamDevice.Mini.V1,
      airBeamFrom("AirBeamMini-1234", emptyList())
    )
  }

  @Test
  fun airbeam3_by_name() {
    assertEquals(
      AirBeamDevice.AirBeam3,
      airBeamFrom("AirBeam3-9", emptyList())
    )
  }

  @Test
  fun null_name_is_null() {
    assertNull(airBeamFrom(null, listOf(MINI_V2_SERVICE)))
  }

  @Test
  fun unknown_is_null() {
    assertNull(airBeamFrom("Some Headphones", emptyList()))
  }

  @Test
  fun dedupes_by_id() = runTest {
    val a1 = DiscoveredAirBeam(DeviceId("A"), "AirBeam3", AirBeamDevice.AirBeam3)
    val a2 = a1.copy(name = "AirBeam3-renamed")
    val b = DiscoveredAirBeam(DeviceId("B"), "AirBeamMini", AirBeamDevice.Mini.V1)
    flowOf(a1, b, a2).accumulateDistinct().test {
      awaitItem()
      awaitItem()
      awaitItem()
      val finalList = awaitItem()
      assertEquals(2, finalList.size)
      assertEquals("AirBeam3-renamed", finalList.first { it.id == DeviceId("A") }.name)
      awaitComplete()
    }
  }
}