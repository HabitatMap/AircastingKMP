package pl.llp.aircasting.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.llp.aircasting.bluetooth.AirBeamSessionController
import pl.llp.aircasting.bluetooth.SessionState

class AirBeamForegroundService : Service(), KoinComponent {

  private val sessionController: AirBeamSessionController by inject()
  private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  companion object {
    private const val CHANNEL_ID = "aircasting_session_channel"
    private const val CHANNEL_NAME = "AirBeam Session"
    private const val NOTIFICATION_ID = 1001

    fun start(context: Context) {
      val intent = Intent(context, AirBeamForegroundService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, AirBeamForegroundService::class.java)
      context.stopService(intent)
    }
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    observeSessionState()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val initialNotification = buildNotification("AirBeam service active")
    startForegroundWithCompat(initialNotification)
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun observeSessionState() {
    sessionController.state
      .dropWhile { it is SessionState.Idle }
      .onEach { state ->
        when (state) {
          SessionState.Idle, is SessionState.Failed -> {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
          }
          is SessionState.Connecting -> updateNotification("Connecting to ${state.target.name}...")
          is SessionState.Connected -> updateNotification("Connected to AirBeam")
          is SessionState.Configuring -> updateNotification("Configuring session...")
          is SessionState.Recording -> updateNotification("Recording active session")
          is SessionState.Reconnecting -> updateNotification("Reconnecting to ${state.target.name} (Attempt ${state.attempt})...")
        }
      }
      .launchIn(serviceScope)
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_LOW
      )
      val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
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

  private fun startForegroundWithCompat(notification: Notification) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }
}
