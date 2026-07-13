package com.lunarlogic.aircasting.bluetooth.transport.ble

import com.juul.kable.Scanner
import com.lunarlogic.aircasting.bluetooth.AirBeamConnection
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.DeviceId
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.detection.airBeamFrom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold

class BleAirBeamConnector : AirBeamConnector {
  override fun scan(): Flow<List<DiscoveredAirBeam>> =
    Scanner().advertisements
      // deduplicate provided list of found devices
      .mapNotNull { adv -> airBeamFrom(adv.name, adv.uuids)?.let {
        DiscoveredAirBeam(DeviceId(adv.identifier.toString()), adv.name ?: "", it)
      } }
      .runningFold(emptyMap<DeviceId, DiscoveredAirBeam>()) { acc, d -> acc + (d.id to d) }
      .map { it.values.toList() }
  override suspend fun connect(target: DiscoveredAirBeam): AirBeamConnection = TODO()
}