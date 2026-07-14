package com.lunarlogic.aircasting.bluetooth.transport.ble

import com.juul.kable.Scanner
import com.lunarlogic.aircasting.bluetooth.AirBeamConnection
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.DeviceId
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.Transport
import com.lunarlogic.aircasting.bluetooth.detection.airBeamFrom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.runningFold

class BleAirBeamConnector : AirBeamConnector {
  override val supportedTransports = setOf(Transport.BLE)

  override fun scan() = Scanner().advertisements
    .mapNotNull { advertisement ->
      airBeamFrom(advertisement.name, advertisement.uuids)?.let { airbeamDevice ->
        DiscoveredAirBeam(
          DeviceId(advertisement.identifier.toString()),
          advertisement.name ?: "",
          airbeamDevice
        )
      }
    }
    .accumulateDistinct()

  override suspend fun connect(target: DiscoveredAirBeam): AirBeamConnection = TODO()
}

fun Flow<DiscoveredAirBeam>.accumulateDistinct(): Flow<List<DiscoveredAirBeam>> =
  runningFold(emptyMap<DeviceId, DiscoveredAirBeam>()) { acc, d -> acc + (d.id to d) }
    .map { it.values.toList() }