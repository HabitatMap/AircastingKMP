package pl.llp.aircasting.bluetooth.transport.ble

import co.touchlab.kermit.Logger
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import pl.llp.aircasting.bluetooth.AirBeamConnection
import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConnectionStatus
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.Transport
import pl.llp.aircasting.bluetooth.detection.airBeamFrom
import pl.llp.aircasting.bluetooth.transport.accumulateDistinct
import pl.llp.aircasting.bluetooth.v2_firmware_specific.DeviceReportedState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private val SCAN_TIMEOUT_DURATION = 10.seconds
private val CONNECT_TIMEOUT_DURATION = 30.seconds
private val HANDSHAKE_SETTLE_DURATION = 500.milliseconds
private val STATUS_SETTLE_TIMEOUT = 5.seconds

class BleAirBeamConnector() : AirBeamConnector {
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

  override suspend fun connect(target: DiscoveredAirBeam): AirBeamConnection {
    val advertisement = withTimeoutOrNull(SCAN_TIMEOUT_DURATION) {
      Scanner().advertisements.first { it.identifier.toString() == target.id.value }
    } ?: return failedConnection(FailureReason.NoDeviceFound)

    val peripheral = Peripheral(advertisement)
    try {
      withTimeout(CONNECT_TIMEOUT_DURATION) { peripheral.connect() }

    } catch (cancel: CancellationException) {
      peripheral.disconnectQuietly()
      if (cancel is TimeoutCancellationException)
        return failedConnection(FailureReason.LinkTimeout)
      throw cancel

    } catch (_: Exception) {
      peripheral.disconnectQuietly()
      return failedConnection(FailureReason.LinkTimeout)
    }

    return try {
      if (target.device is AirBeamDevice.Mini.V2) {
        connectV2(peripheral, target.device)
      } else {
        BleConnection(peripheral, target.device)
      }

    } catch (_: TimeoutCancellationException) {
      peripheral.disconnectQuietly()
      failedConnection(FailureReason.HandshakeFailed)
    } catch (cancel: CancellationException) {
      peripheral.disconnectQuietly()
      throw cancel
    } catch (_: Exception) {
      peripheral.disconnectQuietly()
      failedConnection(FailureReason.HandshakeFailed)
    }
  }

  private suspend fun connectV2(peripheral: Peripheral, device: AirBeamDevice): AirBeamConnection {
    // Scope outlives connect(): keeps observing Status for later transitions (Idle -> Running, etc.).
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    listOf(AirBeamGatt.MiniV2.response, AirBeamGatt.MiniV2.measurement,
      AirBeamGatt.MiniV2.sync).forEach { ch ->
      peripheral.observe(ch).launchIn(scope)
    }
    val states = peripheral.observe(AirBeamGatt.MiniV2.status,)
      .mapNotNull { DeviceReportedState.from(it) }
    // stateIn (suspend overload) subscribes, then suspends until the first frame — that IS the settle.
    val deviceState = withTimeoutOrNull(STATUS_SETTLE_TIMEOUT) { states.stateIn(scope) }

    if (deviceState == null && peripheral.state.value !is State.Connected) {
      scope.cancel()
      peripheral.disconnectQuietly()
      return failedConnection(FailureReason.LinkTimeout)
    }

    return BleConnection(peripheral, device, deviceState, scope)
  }
}


private class BleConnection(
  private val peripheral: Peripheral,
  val device: AirBeamDevice,
  override val deviceState: StateFlow<DeviceReportedState>? = null,
  private val scope: CoroutineScope? = null,
) : AirBeamConnection {
  private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Ready(device))
  override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()
  private val log = Logger.withTag("BleConnection")

  override suspend fun configure(config: SessionConfig): ConfigResult {
    log.i { "Configuring device: $device with config: $config" }
    return try {
      when (device) {
        AirBeamDevice.AirBeam3, AirBeamDevice.Mini.V1 -> {
          oldSetup(config)
        }
        AirBeamDevice.Mini.V2 -> {
          //TOOD: configure abm2
          ConfigResult.Success
        }
        AirBeamDevice.AirBeam2 -> {
          log.e { "BLE connection configure method attempted to configure AirBeam2" }
          ConfigResult.UnknownFailure
        }
      }
    } catch (e: Exception) {
      log.e(e) { "Configuration failed for $device" }
      ConfigResult.UnknownFailure
    }
  }

  override suspend fun disconnect() {
    scope?.cancel()
    peripheral.disconnect()
    _status.value = ConnectionStatus.Disconnected
  }

  private suspend fun oldSetup(config: SessionConfig): ConfigResult {
    sendCommand(AirBeamProtocol.Standard.setTimeCommand())
    sendCommand(AirBeamProtocol.Standard.setUuidCommand(config.uuid))
    when (config) {
      is SessionConfig.Mobile -> {
        sendCommand(AirBeamProtocol.Standard.startMobileCommand())
      }
      is SessionConfig.FixedWiFi -> {
        sendCommand(AirBeamProtocol.Standard.setAuthCommand(config.authToken))
        sendCommand(AirBeamProtocol.Standard.setLocationCommand(config.latitude, config.longitude))
        sendCommand(AirBeamProtocol.Standard.startFixedCommand(config.ssid, config.password, config.zoneOffset))
      }
      is SessionConfig.FixedCellular -> {
        sendCommand(AirBeamProtocol.Standard.setAuthCommand(config.authToken))
        sendCommand(AirBeamProtocol.Standard.setLocationCommand(config.latitude, config.longitude))
        sendCommand(AirBeamProtocol.Standard.startFixedCellCommand())
      }
    }
    return ConfigResult.Success
  }
  private suspend fun sendCommand(command: ByteArray) {
    peripheral.write(AirBeamGatt.Standard.config, command, WriteType.WithResponse)
    delay(500.milliseconds) //give time to AirBeam to process the command as in the old app
  }
}

private fun failedConnection(reason: FailureReason): AirBeamConnection =
  object : AirBeamConnection {
    override val status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Failed(reason))
    override val deviceState = null
    override suspend fun configure(config: SessionConfig) = ConfigResult.UnknownFailure
    override suspend fun disconnect() {}
  }

private suspend fun Peripheral.disconnectQuietly() {
  try {
    disconnect()
  } catch (_: Exception) {
  }
}


object AirBeamGatt {
  // AB3 and Mini V1 share the same protocol/characteristics
  object Standard {
    private const val BASE = "-0000-1000-8000-00805f9b34fb"
    val service = Uuid.parse("0000ffdd$BASE")
    val config = characteristicOf(service, Uuid.parse("0000ffde$BASE"))
    val sdSync = characteristicOf(service, Uuid.parse("0000ffdf$BASE"))
    val temperatureC = characteristicOf(service, Uuid.parse("0000ffe0$BASE"))
    val temperatureF = characteristicOf(service, Uuid.parse("0000ffe1$BASE"))
    val humidity = characteristicOf(service, Uuid.parse("0000ffe3$BASE"))
    val PM1 = characteristicOf(service, Uuid.parse("0000ffe4$BASE"))
    val PM2_5 = characteristicOf(service, Uuid.parse("0000ffe5$BASE"))
    val PM10 = characteristicOf(service, Uuid.parse("0000ffe6$BASE"))
  }

  object MiniV2 {
    private const val BASE = "-4b3c-8e9a-1f2d3c4b5a60"
    val service = Uuid.parse("a0e1f000-0001$BASE")
    val status = characteristicOf(service, Uuid.parse("a0e1f000-0002$BASE"))
    val command = characteristicOf(service, Uuid.parse("a0e1f000-0004$BASE"))
    val response = characteristicOf(service, Uuid.parse("a0e1f000-0004$BASE"))
    val measurement = characteristicOf(service, Uuid.parse("a0e1f000-0005$BASE"))
    val sync = characteristicOf(service, Uuid.parse("a0e1f000-0006$BASE"))
  }
}
