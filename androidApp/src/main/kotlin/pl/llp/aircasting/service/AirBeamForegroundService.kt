package pl.llp.aircasting.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.LinkState

/**
 * Keeps the process alive while the AirBeam link is up. It holds no connection itself — the
 * shared [AirBeamLink] does; this only mirrors its state and stops when the link ends.
 */
class AirBeamForegroundService : Service(), KoinComponent {

  private val link: AirBeamLink by inject()
  private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  companion object {
    private const val CHANNEL_ID = "aircasting_session_channel"
    private const val CHANNEL_NAME = "AirBeam Session"
    private const val NOTIFICATION_ID = 1001

    fun start(context: Context) {
      context.startForegroundService(Intent(context, AirBeamForegroundService::class.java))
    }

    fun stop(context: Context) {
      val intent = Intent(context, AirBeamForegroundService::class.java)
      context.stopService(intent)
    }
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    observeLink()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val initialNotification = buildNotification("AirBeam service active")
    // ServiceCompat drops the service type below API 29, where the overload doesn't exist.
    ServiceCompat.startForeground(
      this, NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
    )
    // A restarted process has an idle link and nothing to restore it from — don't come back.
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun observeLink() {
    link.state
      .dropWhile { it is LinkState.Idle }
      .onEach { state ->
        when (state) {
          LinkState.Idle, is LinkState.Failed -> {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
          }
          is LinkState.Connecting -> updateNotification("Connecting to ${state.target.name}...")
          is LinkState.Connected -> updateNotification("Connected to ${state.target.name}")
          is LinkState.Reconnecting -> updateNotification("Reconnecting to ${state.target.name} (Attempt ${state.attempt})...")
        }
      }
      .launchIn(serviceScope)
  }

  private fun createNotificationChannel() {
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
  }

  private fun buildNotification(contentText: String): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("AirCasting")
      .setContentText(contentText)
      .setSmallIcon(android.R.drawable.ic_menu_compass)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  @SuppressLint("MissingPermission")
  private fun updateNotification(contentText: String) {
    val notification = buildNotification(contentText)
    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(NOTIFICATION_ID, notification)
  }
}
