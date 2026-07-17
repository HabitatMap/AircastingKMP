package com.lunarlogic.aircasting.bluetooth.transport

import com.lunarlogic.aircasting.bluetooth.AirBeamConnection
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.ConnectionStatus
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.Transport
import com.lunarlogic.aircasting.bluetooth.v2_firmware_specific.DeviceReportedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeConnector(
  override val supportedTransports: Set<Transport>,
  private val scanFlow: Flow<List<DiscoveredAirBeam>> = flowOf(emptyList()),
  private val connection: AirBeamConnection = FakeConnection,
) : AirBeamConnector {
  var connectedTarget: DiscoveredAirBeam? = null
  override fun scan() = scanFlow
  override suspend fun connect(target: DiscoveredAirBeam): AirBeamConnection {
    connectedTarget = target
    return connection
  }
}

object FakeConnection : AirBeamConnection {
  override val status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
  override val deviceState = null
  override suspend fun disconnect() {}
}

class ControllableConnection(
  override val status: MutableStateFlow<ConnectionStatus>,
  override val deviceState: MutableStateFlow<DeviceReportedState>? = null,
) : AirBeamConnection {
  var disconnectCalled = false
  override suspend fun disconnect() {
    disconnectCalled = true
  }
}
