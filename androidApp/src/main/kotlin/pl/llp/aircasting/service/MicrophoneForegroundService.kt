package pl.llp.aircasting.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import pl.llp.aircasting.recording.BackgroundSessionHost

/**
 * Keeps a phone-mic session recording in the background. Separate from [AirBeamForegroundService]:
 * that one ends with the AirBeam link, and Android 14 only lets a `microphone`-type service use
 * the mic while the app isn't visible. Legacy had the same split (MicrophoneService).
 */
class MicrophoneForegroundService : Service() {
  companion object {
    private const val CHANNEL_ID = "aircasting_microphone_channel"
    private const val NOTIFICATION_ID = 1002

    fun start(context: Context) {
      context.startForegroundService(Intent(context, MicrophoneForegroundService::class.java))
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, MicrophoneForegroundService::class.java))
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(
      NotificationChannel(CHANNEL_ID, "Phone microphone session", NotificationManager.IMPORTANCE_LOW),
    )
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("AirCasting")
      .setContentText("Recording sound levels")
      .setSmallIcon(android.R.drawable.ic_btn_speak_now)
      .setOngoing(true)
      .build()
    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType())
    return START_NOT_STICKY
  }

  /** Location only with permission: Android 14 throws when asked for it without. */
  private fun serviceType(): Int {
    val location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
      PackageManager.PERMISSION_GRANTED
    return ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
      (if (location) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
  }

  override fun onBind(intent: Intent?): IBinder? = null
}

class MicrophoneServiceHost(private val context: Context) : BackgroundSessionHost {
  override fun start() = MicrophoneForegroundService.start(context)
  override fun stop() = MicrophoneForegroundService.stop(context)
}