package com.lunarlogic.aircasting.bluetooth.transport.classic

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
import com.lunarlogic.aircasting.bluetooth.AirBeamConnection
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.AirBeamDevice
import com.lunarlogic.aircasting.bluetooth.ConnectionStatus
import com.lunarlogic.aircasting.bluetooth.DeviceId
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.FailureReason
import com.lunarlogic.aircasting.bluetooth.Transport
import com.lunarlogic.aircasting.bluetooth.handshake.HandshakeMessages
import com.lunarlogic.aircasting.bluetooth.transport.accumulateDistinct
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
import kotlinx.io.IOException
import java.util.UUID
import kotlin.let
import kotlin.run
import kotlin.time.Duration.Companion.milliseconds

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val CONNECT_TIMEOUT_MS = 30_000L
private const val HANDSHAKE_SETTLE_MS = 3_000L

class ClassicAirBeamConnector(
  private val context: Context,
  private val adapter: BluetoothAdapter?,
  private val credentials: AirBeam2Credentials,
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
          handshake(socket)     // identity → settle → auth
        }
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

  private suspend fun handshake(socket: BluetoothSocket) {
    val output = socket.outputStream

    withContext(Dispatchers.IO) {
      output.write(HandshakeMessages.uuidMessage(credentials.sessionUuid()))
      output.flush()
    }

    delay(HANDSHAKE_SETTLE_MS.milliseconds)

    withContext(Dispatchers.IO) {
      output.write(HandshakeMessages.authTokenMessage(credentials.authToken()))
      output.flush()
    }
  }
}

private class ClassicConnection(
  private val socket: BluetoothSocket,
  device: AirBeamDevice,
) : AirBeamConnection {
  private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Ready(device))
  override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()
  override val deviceState = null // AB2 does not report its own state

  override suspend fun disconnect() {
    withContext(Dispatchers.IO) { socket.safeClose() }
    _status.value = ConnectionStatus.Disconnected
  }
}

private fun failed(reason: FailureReason): AirBeamConnection = object : AirBeamConnection {
  override val status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Failed(reason))
  override val deviceState = null
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

interface AirBeam2Credentials {
  suspend fun sessionUuid(): String // identity, written first  (0x04)
  suspend fun authToken(): String   // credential, written second (0x05)
}