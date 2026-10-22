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
import pl.llp.aircasting.bluetooth.ConnectResult
import pl.llp.aircasting.bluetooth.ConnectionStatus
import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import pl.llp.aircasting.bluetooth.SensorReading
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.bluetooth.Transport
import pl.llp.aircasting.bluetooth.protocol.AirBeamProtocol
import pl.llp.aircasting.bluetooth.transport.accumulateDistinct
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
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
  override suspend fun connect(target: DiscoveredAirBeam): ConnectResult {
    val adapter = adapter ?: return ConnectResult.Failed(FailureReason.RadioOrPermissionMissing)
    adapter.cancelDiscovery() // inquiry saturates the radio — always stop before connecting

    val socket = adapter.getRemoteDevice(target.id.value)
      .createRfcommSocketToServiceRecord(SPP_UUID)

    return coroutineScope {
      // socket.connect() blocks and ignores cancellation; closing the socket is what aborts it.
      val watchdog = launch {
        delay(CONNECT_TIMEOUT_MS.milliseconds)
        socket.safeClose()
      }
      try {
        runInterruptible(Dispatchers.IO) { socket.connect() }
        watchdog.cancel()
        delay(ESTIMATED_CONNECTING_TIME_MS.milliseconds)
        ConnectResult.Connected(ClassicConnection(socket, target.device))
      } catch (_: IOException) {
        val timedOut = watchdog.isCompleted // ran to the end: it closed the socket on us
        watchdog.cancel()
        socket.safeClose()
        ConnectResult.Failed(if (timedOut) FailureReason.LinkTimeout else FailureReason.HandshakeFailed)
      } catch (cancel: CancellationException) {
        socket.safeClose()
        throw cancel
      }
    }
  }
}

private class ClassicConnection(
  private val socket: BluetoothSocket,
  override val device: AirBeamDevice,
) : AirBeamConnection {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val _status = MutableStateFlow(ConnectionStatus.Ready)
  override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()
  override val deviceState = null // AB2 does not report its own state
  private val log = Logger.withTag("ClassicConnection")
  private val received = Channel<SensorReading>(Channel.UNLIMITED)
  override val readings = received.receiveAsFlow()

  init {
    // AB2 streams one ASCII measurement line at a time (legacy AirBeam2Reader).
    scope.launch {
      try {
        val lines = socket.inputStream.bufferedReader()
        while (true) {
          val line = runInterruptible { lines.readLine() } ?: break
          log.v { line }
          AirBeamProtocol.Standard.parseReading(line)?.let { received.trySend(it) }
        }
      } catch (e: IOException) {
        log.d { "Classic socket read loop ended: ${e.message}" }
      } finally {
        received.close()
        if (_status.compareAndSet(ConnectionStatus.Ready, ConnectionStatus.DisconnectedUnexpectedly)) {
          log.w { "Classic connection dropped unexpectedly" }
        }
      }
    }
  }

  override suspend fun configure(config: SessionConfig): ConfigResult {
    log.i { "Configuring AirBeam2 ($device) with $config" }
    return try {
      when (config) {
        is SessionConfig.Mobile -> configureMobile(config)
        is SessionConfig.FixedWiFi -> configureFixedWiFi(config)
        is SessionConfig.FixedCellular -> configureFixedCellular(config)
      }
    } catch (e: Exception) {
      log.e(e) { "Configuration failed for AirBeam2" }
      ConfigResult.UnknownFailure
    }
  }

  /** Legacy `AirBeam2Configurator.reconnectMobileSession`: mobile mode only; the UUID is already set. */
  override suspend fun resumeMobileSession(config: SessionConfig.Mobile): ConfigResult = try {
    sendCommand(AirBeamProtocol.Standard.startMobileCommand())
    ConfigResult.Success
  } catch (e: IOException) {
    log.e(e) { "Resuming mobile mode failed for AirBeam2" }
    ConfigResult.UnknownFailure
  }

  /**
   * Legacy `AirBeam2Configurator` (UUID, 3 s, then mobile mode), framed `0xFE … 0xFF` like every
   * V1 device (`HexMessagesBuilder`) — hence `Standard`, not `AirBeam2`. Legacy also sends the
   * auth token, but that's only for the device's own uploads in fixed sessions: the hardware run
   * (2026-10-22) streamed at 1 Hz without it.
   */
  private suspend fun configureMobile(config: SessionConfig.Mobile): ConfigResult {
    sendCommand(AirBeamProtocol.Standard.setUuidCommand(config.uuid))
    delay(HANDSHAKE_SETTLE_MS.milliseconds)
    sendCommand(AirBeamProtocol.Standard.startMobileCommand())
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
    _status.compareAndSet(ConnectionStatus.Ready, ConnectionStatus.Disconnected)
    scope.cancel()
    withContext(Dispatchers.IO) { socket.safeClose() }
  }
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