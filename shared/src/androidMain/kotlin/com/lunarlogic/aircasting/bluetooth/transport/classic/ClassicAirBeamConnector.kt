package com.lunarlogic.aircasting.bluetooth.transport.classic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.lunarlogic.aircasting.bluetooth.AirBeamConnection
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.AirBeamDevice
import com.lunarlogic.aircasting.bluetooth.DeviceId
import com.lunarlogic.aircasting.bluetooth.DiscoveredAirBeam
import com.lunarlogic.aircasting.bluetooth.Transport
import com.lunarlogic.aircasting.bluetooth.transport.accumulateDistinct
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.let
import kotlin.run

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

  override suspend fun connect(target: DiscoveredAirBeam): AirBeamConnection = TODO("3b-ii")
}

@SuppressLint("MissingPermission")
private fun airBeam2From(device: BluetoothDevice): DiscoveredAirBeam? {
  val name = device.name ?: return null
  if (!name.contains("airbeam2", ignoreCase = true)) return null
  return DiscoveredAirBeam(DeviceId(device.address), name, AirBeamDevice.AirBeam2)
}