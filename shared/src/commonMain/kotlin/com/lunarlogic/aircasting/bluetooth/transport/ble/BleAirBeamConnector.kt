package com.lunarlogic.aircasting.bluetooth.transport.ble

import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import com.lunarlogic.aircasting.bluetooth.AirBeamConnection
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.AirBeamCredentials
import com.lunarlogic.aircasting.bluetooth.AirBeamDevice
import com.lunarlogic.aircasting.bluetooth.ConnectionStatus
import com.lunarlogic.aircasting.bluetooth.DeviceId
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.FailureReason
import com.lunarlogic.aircasting.bluetooth.Transport
import com.lunarlogic.aircasting.bluetooth.detection.airBeamFrom
import com.lunarlogic.aircasting.bluetooth.handshake.HandshakeMessages
import com.lunarlogic.aircasting.bluetooth.transport.accumulateDistinct
import com.lunarlogic.aircasting.bluetooth.v2_firmware_specific.DeviceReportedState
import com.lunarlogic.aircasting.bluetooth.v2_firmware_specific.MINI_V2_SERVICE
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

private val SCAN_TIMEOUT_DURATION = 10.seconds
private val CONNECT_TIMEOUT_DURATION = 30.seconds
private val HANDSHAKE_SETTLE_DURATION = 500.milliseconds

// AB3 + Mini V1 share this GATT service + config (write) characteristic.
private val configCharacteristic = characteristicOf(
  Uuid.parse("0000ffdd-0000-1000-8000-00805f9b34fb"),
  Uuid.parse("0000ffde-0000-1000-8000-00805f9b34fb")
)

// Mini V2: dedicated Status characteristic (notify)
private val statusCharacteristic = characteristicOf(
  MINI_V2_SERVICE,
  Uuid.parse("a0e1f000-0002-4b3c-8e9a-1f2d3c4b5a60"),
)

private val STATUS_SETTLE_TIMEOUT = 5.seconds

class BleAirBeamConnector(
  private val credentials: AirBeamCredentials,
) : AirBeamConnector {
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
      if (target.device.requiresHandshake) {
        handshake(peripheral)
        BleConnection(peripheral, target.device)
      } else {
        connectV2(peripheral, target.device)
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
    val states = peripheral.observe(statusCharacteristic)
      .mapNotNull { DeviceReportedState.from(it) }
    // stateIn (suspend overload) subscribes, then suspends until the first frame — that IS the settle.
    val deviceState = try {
      withTimeout(STATUS_SETTLE_TIMEOUT) { states.stateIn(scope) }
    } catch (e: Exception) {
      scope.cancel()
      throw e
    }
    return BleConnection(peripheral, device, deviceState, scope)
  }

  private suspend fun handshake(peripheral: Peripheral) {
    peripheral.write(
      configCharacteristic,
      HandshakeMessages.uuidMessage(credentials.sessionUuid()),
      WriteType.WithResponse,
    )
    delay(HANDSHAKE_SETTLE_DURATION)
    peripheral.write(
      configCharacteristic,
      HandshakeMessages.authTokenMessage(credentials.authToken()),
      WriteType.WithResponse,
    )
  }
}


private class BleConnection(
  private val peripheral: Peripheral,
  device: AirBeamDevice,
  override val deviceState: StateFlow<DeviceReportedState>? = null,
  private val scope: CoroutineScope? = null,
) : AirBeamConnection {
  private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Ready(device))
  override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

  override suspend fun disconnect() {
    scope?.cancel()
    peripheral.disconnect()
    _status.value = ConnectionStatus.Disconnected
  }
}

private fun failedConnection(reason: FailureReason): AirBeamConnection =
  object : AirBeamConnection {
    override val status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Failed(reason))
    override val deviceState = null
    override suspend fun disconnect() {}
  }

private suspend fun Peripheral.disconnectQuietly() {
  try {
    disconnect()
  } catch (_: Exception) {
  }
}