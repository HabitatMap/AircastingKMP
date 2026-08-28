package pl.llp.aircasting.bluetooth.transport.classic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import co.touchlab.kermit.Logger
import pl.llp.aircasting.bluetooth.AirBeamConnection
import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.ConnectionStatus
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.bluetooth.Transport
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol
import pl.llp.aircasting.bluetooth.transport.accumulateDistinct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.UUID
import kotlin.let
import kotlin.run
import kotlin.time.Duration.Companion.milliseconds

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val CONNECT_TIMEOUT_MS = 30_000L
private const val ESTIMATED_CONNECTING_TIME_MS = 5_000L
private const val HANDSHAKE_SETTLE_MS = 3_000L

class ClassicAirBeamConnector(
  private val context: Context,
  private val adapter: BluetoothAdapter?,
) : AirBeamConnector {
  override val supportedTransports = setOf(Transport.CLASSIC_SERIAL)

  @SuppressLint("MissingPermission") // BLUETOOTH_SCAN/CONNECT are a precondition, gated upstream
  override fun scan(): Flow<List<DiscoveredAirBeam>> = callbackFlow {
    val adapter = adapter
      ?: run { close(); return@callbackFlow } // no radio → empty, terminates

    fun offer(device: BluetoothDevice) {
      airBeam2From(device)?.let { trySend(it) }
    }

    // Already-paired devices seed the list right away — no need to wait for a scan hit.
    adapter.bondedDevices.orEmpty()
      .forEach(::offer)

    val receiver = object : BroadcastReceiver() {
      override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_FOUND) return
        IntentCompat.getParcelableExtra(
          intent,
          BluetoothDevice.EXTRA_DEVICE,
          BluetoothDevice::class.java
        )
          ?.let(::offer)
      }
    }
    ContextCompat.registerReceiver(
      context, receiver, IntentFilter(BluetoothDevice.ACTION_FOUND),
      ContextCompat.RECEIVER_EXPORTED,
    )
    adapter.startDiscovery()
    awaitClose {
      adapter.cancelDiscovery()
      context.unregisterReceiver(receiver)
    }
  }.accumulateDistinct()

  @SuppressLint("MissingPermission")
  override suspend fun connect(target: DiscoveredAirBeam): AirBeamConnection {
    val adapter = adapter ?: return failed(FailureReason.RadioOrPermissionMissing)
    adapter.cancelDiscovery() // inquiry saturates the radio — always stop before connecting

    val socket = adapter.getRemoteDevice(target.id.value)
      .createRfcommSocketToServiceRecord(SPP_UUID)

    return withContext(Dispatchers.IO) {
      try {
        withTimeout(CONNECT_TIMEOUT_MS.milliseconds) {
          socket.connect()      // blocking until linked or IOException
        }
        delay(ESTIMATED_CONNECTING_TIME_MS.milliseconds)
        ClassicConnection(socket, target.device)
      } catch (e: TimeoutCancellationException) {
        socket.safeClose()
        failed(FailureReason.LinkTimeout)
      } catch (_: IOException) {
        socket.safeClose()
        failed(FailureReason.HandshakeFailed)
      }
    }
  }
}

private class ClassicConnection(
  private val socket: BluetoothSocket,
  val device: AirBeamDevice,
) : AirBeamConnection {
  private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Ready(device))
  override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()
  override val deviceState = null // AB2 does not report its own state
  private val log = Logger.withTag("ClassicConnection")

  override suspend fun configure(config: SessionConfig): ConfigResult {
    log.i { "Configuring AirBeam2 ($device) with config: $config" }
    return try {
      when (config) {
        is SessionConfig.Mobile -> configureMobile()
        is SessionConfig.FixedWiFi -> configureFixedWiFi(config)
        is SessionConfig.FixedCellular -> configureFixedCellular(config)
      }
    } catch (e: Exception) {
      log.e(e) { "Configuration failed for AirBeam2" }
      ConfigResult.UnknownFailure
    }
  }

  private suspend fun configureMobile(): ConfigResult {
    // Mobile Session (Bluetooth): Send [0x01, 0xFF]
    sendCommand(AirBeamProtocol.AirBeam2.startMobileCommand())
    return ConfigResult.Success
  }

  private suspend fun configureFixedWiFi(config: SessionConfig.FixedWiFi): ConfigResult {
    // 1. Send [0x04] + "<UUID>" + [0xFF]
    sendCommand(AirBeamProtocol.AirBeam2.setUuidCommand(config.uuid))
    delay(HANDSHAKE_SETTLE_MS.milliseconds)

    // 2. Send [0x05] + "<UUID_AUTH>" + [0xFF]
    sendCommand(AirBeamProtocol.AirBeam2.setAuthCommand(config.authToken))
    delay(HANDSHAKE_SETTLE_MS.milliseconds)

    // 3. Send [0x06] + "<LONGITUDE>,<LATITUDE>" + [0xFF]
    sendCommand(AirBeamProtocol.AirBeam2.setLocationCommand(config.latitude, config.longitude))
    delay(HANDSHAKE_SETTLE_MS.milliseconds)

    // 4. Send [0x02] + "<SSID>,<PASSWORD>,<ZONE_OFFSET>" + [0xFF]
    sendCommand(AirBeamProtocol.AirBeam2.startFixedWifiCommand(config.ssid, config.password, config.zoneOffset))
    return ConfigResult.Success
  }

  private suspend fun configureFixedCellular(config: SessionConfig.FixedCellular): ConfigResult {
    // 1. Send [0x04] + "<UUID>" + [0xFF]
    sendCommand(AirBeamProtocol.AirBeam2.setUuidCommand(config.uuid))
    delay(HANDSHAKE_SETTLE_MS.milliseconds)

    // 2. Send [0x05] + "<UUID_AUTH>" + [0xFF]
    sendCommand(AirBeamProtocol.AirBeam2.setAuthCommand(config.authToken))
    delay(HANDSHAKE_SETTLE_MS.milliseconds)

    // 3. Send [0x06] + "<LONGITUDE>,<LATITUDE>" + [0xFF]
    sendCommand(AirBeamProtocol.AirBeam2.setLocationCommand(config.latitude, config.longitude))
    delay(HANDSHAKE_SETTLE_MS.milliseconds)

    // 4. Send [0x03, 0xFF]
    sendCommand(AirBeamProtocol.AirBeam2.startFixedCellCommand())
    return ConfigResult.Success
  }

  private suspend fun sendCommand(command: ByteArray) {
    withContext(Dispatchers.IO) {
      socket.outputStream.write(command)
      socket.outputStream.flush()
    }
  }

  override suspend fun disconnect() {
    withContext(Dispatchers.IO) { socket.safeClose() }
    _status.value = ConnectionStatus.Disconnected
  }
}

private fun failed(reason: FailureReason): AirBeamConnection = object : AirBeamConnection {
  override val status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Failed(reason))
  override val deviceState = null
  override suspend fun configure(config: SessionConfig) = ConfigResult.UnknownFailure
  override suspend fun disconnect() {}
}

private fun BluetoothSocket.safeClose() {
  try {
    close()
  } catch (_: IOException) {
  }
}

@SuppressLint("MissingPermission")
private fun airBeam2From(device: BluetoothDevice): DiscoveredAirBeam? {
  val name = device.name ?: return null
  if (!name.contains("airbeam2", ignoreCase = true)) return null
  return DiscoveredAirBeam(DeviceId(device.address), name, AirBeamDevice.AirBeam2)
}