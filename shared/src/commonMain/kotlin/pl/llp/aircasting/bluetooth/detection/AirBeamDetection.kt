package pl.llp.aircasting.bluetooth.detection

import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.transport.ble.AirBeamGatt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class)
fun airBeamFrom(name: String?, serviceUuids: List<Uuid>): AirBeamDevice? = when {
  name == null -> null
  AirBeamGatt.MiniV2.service in serviceUuids -> AirBeamDevice.Mini.V2
  name.contains("airbeammini", true) -> AirBeamDevice.Mini.V1
  name.contains("airbeam3", true) -> AirBeamDevice.AirBeam3
  // AB2 never appears in a BLE scan — no case here
  else -> null
}