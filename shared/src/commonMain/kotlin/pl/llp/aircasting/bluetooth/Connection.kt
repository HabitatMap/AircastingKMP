package pl.llp.aircasting.bluetooth

import pl.llp.aircasting.bluetooth.v2_firmware_specific.DeviceReportedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pl.llp.aircasting.domain.GeoLocation
import kotlin.uuid.Uuid

interface AirBeamConnector {
  val supportedTransports: Set<Transport>
  fun scan(): Flow<List<DiscoveredAirBeam>>

  /** Link + handshake. Device and radio trouble is a [ConnectResult.Failed], never a throw. */
  suspend fun connect(target: DiscoveredAirBeam): ConnectResult
}

sealed interface ConnectResult {
  data class Connected(val connection: AirBeamConnection) : ConnectResult
  data class Failed(val reason: FailureReason) : ConnectResult
}

/**
 * One live, handshaken link. Holds platform resources (GATT client, socket) until
 * [disconnect] — call it even after the link dropped by itself.
 */
interface AirBeamConnection {
  val device: AirBeamDevice

  /** Starts [ConnectionStatus.Ready] and leaves it once; a connection never comes back. */
  val status: StateFlow<ConnectionStatus>
  val deviceState: StateFlow<DeviceReportedState>?

  /**
   * Everything the device measures — live, and for Mini V2 the stored backlog it streams after
   * a reconnect. Buffered from connect on, so nothing is lost before the first collector; meant
   * for exactly one collector.
   */
  val readings: Flow<SensorReading>

  suspend fun configure(config: SessionConfig): ConfigResult

  /**
   * Picks up a mobile session that was running before the link dropped. Not [configure]
   * again: Mini V2 still runs its session and would reject a new config.
   */
  suspend fun resumeMobileSession(config: SessionConfig.Mobile): ConfigResult

  suspend fun disconnect()
}

enum class ConnectionStatus {
  Ready,
  /** We called [AirBeamConnection.disconnect]. */
  Disconnected,
  /** The link dropped on its own: out of range, battery, radio off. */
  DisconnectedUnexpectedly,
}

sealed interface FailureReason {
  data object NoDeviceFound: FailureReason
  data object LinkTimeout: FailureReason
  data object WrongCommunicationSurface: FailureReason
  data object HandshakeFailed: FailureReason
  data object RadioOrPermissionMissing: FailureReason
  /** A ready link dropped with no session to reconnect for. */
  data object ConnectionLost: FailureReason
}

sealed interface ConfigResult {
  data object Success: ConfigResult
  data object UnknownFailure: ConfigResult
  data object WifiPassFailure: ConfigResult
  data object BadConfigFailure: ConfigResult
  data object NotSyncedFailure: ConfigResult
  /** Mini V2 fixed: the device never confirmed its first measurement POST (Nack 0x02 or no Ready in time). */
  data object FirstMeasurementFailure: ConfigResult
  /** Nothing to configure: no ready link at the time of the call. */
  data object NotConnected: ConfigResult
}

sealed interface SessionConfig {
  val uuid: Uuid

  // Configs end up in logs; each toString() leaves the credentials out.

  data class Mobile(
    override val uuid: Uuid,
    val authToken: String? = null,
    /** V1 devices only (AB3 streams slowly without one); Mini V2 ignores it. */
    val location: GeoLocation? = null,
  ) : SessionConfig {
    override fun toString() = "Mobile(uuid=$uuid)"
  }

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
  ) : SessionConfig {
    override fun toString() = "FixedWiFi(uuid=$uuid, ssid=$ssid)"
  }

  data class FixedCellular(
    override val uuid: Uuid,
    val authToken: String,
    val latitude: Double,
    val longitude: Double
  ) : SessionConfig {
    override fun toString() = "FixedCellular(uuid=$uuid)"
  }
}