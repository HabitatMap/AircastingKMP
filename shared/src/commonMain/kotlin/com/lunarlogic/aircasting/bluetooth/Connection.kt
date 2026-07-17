package com.lunarlogic.aircasting.bluetooth

import com.lunarlogic.aircasting.bluetooth.v2_firmware_specific.DeviceReportedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AirBeamConnector {
  val supportedTransports: Set<Transport>
  fun scan(): Flow<List<DiscoveredAirBeam>>
  suspend fun connect(target: DiscoveredAirBeam): AirBeamConnection
}

interface AirBeamConnection {
  val status: StateFlow<ConnectionStatus>
  val deviceState: StateFlow<DeviceReportedState>?
  suspend fun disconnect()
}

sealed interface ConnectionStatus {
  data object Disconnected : ConnectionStatus
  data object Connecting : ConnectionStatus
  data class Ready(val device: AirBeamDevice) : ConnectionStatus
  data class Failed(val reason: FailureReason) : ConnectionStatus
  data object DisconnectedUnexpectedly : ConnectionStatus
}

sealed interface FailureReason {
  data object NoDeviceFound: FailureReason
  data object LinkTimeout: FailureReason
  data object WrongCommunicationSurface: FailureReason
  data object HandshakeFailed: FailureReason
  data object RadioOrPermissionMissing: FailureReason
}