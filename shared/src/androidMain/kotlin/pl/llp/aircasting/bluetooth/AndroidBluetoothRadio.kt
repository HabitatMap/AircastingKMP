package pl.llp.aircasting.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidBluetoothRadio(
  private val context: Context,
  private val adapter: BluetoothAdapter?,
) : BluetoothRadio {

  override val isOff: Flow<Boolean> = callbackFlow {
    trySend(adapter?.isEnabled != true)
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(ctx: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        trySend(state != BluetoothAdapter.STATE_ON)
      }
    }
    // System broadcasts still reach NOT_EXPORTED receivers.
    ContextCompat.registerReceiver(
      context, receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    awaitClose { context.unregisterReceiver(receiver) }
  }.distinctUntilChanged()

  @SuppressLint("MissingPermission") // BLUETOOTH_CONNECT is granted before this screen can show
  override fun requestEnable() {
    context.startActivity(
      Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
  }
}