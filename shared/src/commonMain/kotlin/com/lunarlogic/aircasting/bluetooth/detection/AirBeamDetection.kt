package com.lunarlogic.aircasting.bluetooth.detection

import com.lunarlogic.aircasting.bluetooth.AirBeamDevice
import com.lunarlogic.aircasting.bluetooth.v2_firmware_specific.MINI_V2_SERVICE
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class)
fun airBeamFrom(name: String?, serviceUuids: List<Uuid>): AirBeamDevice? = when {
  name == null -> null
  MINI_V2_SERVICE in serviceUuids -> AirBeamDevice.Mini.V2
  name.contains("airbeammini", true) -> AirBeamDevice.Mini.V1
  name.contains("airbeam3", true) -> AirBeamDevice.AirBeam3
  // AB2 never appears in a BLE scan — no case here
  else -> null
}