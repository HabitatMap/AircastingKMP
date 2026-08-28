package pl.llp.aircasting.bluetooth

import pl.llp.aircasting.bluetooth.v2_firmware_specific.DeviceReportedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.Uuid

interface AirBeamConnector {
  val supportedTransports: Set<Transport>
  fun scan(): Flow<List<DiscoveredAirBeam>>
  suspend fun connect(target: DiscoveredAirBeam): AirBeamConnection
}

interface AirBeamConnection {
  val status: StateFlow<ConnectionStatus>
  val deviceState: StateFlow<DeviceReportedState>?
  suspend fun configure(config: SessionConfig): ConfigResult
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

sealed interface ConfigResult {
  data object Success: ConfigResult
  data object UnknownFailure: ConfigResult
  data object WifiPassFailure: ConfigResult
  data object BadConfigFailure: ConfigResult
  data object NotSyncedFailure: ConfigResult
}

sealed interface SessionConfig {
  val uuid: Uuid

  data class Mobile(
    override val uuid: Uuid,
    val authToken: String? = null
  ) : SessionConfig

  data class FixedWiFi(
    override val uuid: Uuid,
    val authToken: String,
    val latitude: Double,
    val longitude: Double,
    val ssid: String,
    val password: String,
    val zoneOffset: Int,
    val pm1Index: Int = 0,
    val pm25Index: Int = 1
  ) : SessionConfig

  data class FixedCellular(
    override val uuid: Uuid,
    val authToken: String,
    val latitude: Double,
    val longitude: Double
  ) : SessionConfig
}