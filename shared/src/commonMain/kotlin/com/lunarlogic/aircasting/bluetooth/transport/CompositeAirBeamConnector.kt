package com.lunarlogic.aircasting.bluetooth.transport

import com.lunarlogic.aircasting.bluetooth.AirBeamConnection
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CompositeAirBeamConnector(
  private val delegates: List<AirBeamConnector>,
) : AirBeamConnector {
  override val supportedTransports = delegates
    .flatMap { it.supportedTransports }
    .toSet()

  override fun scan(): Flow<List<DiscoveredAirBeam>> =
    combine(delegates.map { it.scan() }) { lists ->
      lists.toList()
        .flatten()
    }

  override suspend fun connect(target: DiscoveredAirBeam): AirBeamConnection =
    delegates.firstOrNull { target.device.transport in it.supportedTransports }
      ?.connect(target)
      ?: error("No connector for transport ${target.device.transport}")
}