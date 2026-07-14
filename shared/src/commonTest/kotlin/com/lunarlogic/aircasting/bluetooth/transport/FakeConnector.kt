package com.lunarlogic.aircasting.bluetooth.transport

import com.lunarlogic.aircasting.bluetooth.AirBeamConnection
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.ConnectionStatus
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.Transport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeConnector(
  override val supportedTransports: Set<Transport>,
  private val scanFlow: Flow<List<DiscoveredAirBeam>> = flowOf(emptyList()),
) : AirBeamConnector {
  var connectedTarget: DiscoveredAirBeam? = null    // spy for routing test
  override fun scan() = scanFlow
  override suspend fun connect(target: DiscoveredAirBeam): AirBeamConnection {
    connectedTarget = target
    return FakeConnection
  }
}

object FakeConnection : AirBeamConnection {
  override val status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
  override val deviceState = null
  override suspend fun disconnect() {}
}
