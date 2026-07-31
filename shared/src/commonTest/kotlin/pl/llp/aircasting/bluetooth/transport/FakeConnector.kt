package pl.llp.aircasting.bluetooth.transport

import pl.llp.aircasting.bluetooth.AirBeamConnection
import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.ConnectionStatus
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.Transport
import pl.llp.aircasting.bluetooth.v2_firmware_specific.DeviceReportedState
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
