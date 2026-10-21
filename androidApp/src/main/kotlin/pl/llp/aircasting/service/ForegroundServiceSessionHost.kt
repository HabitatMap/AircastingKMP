package pl.llp.aircasting.service

import android.content.Context
import pl.llp.aircasting.recording.BackgroundSessionHost

class ForegroundServiceSessionHost(private val context: Context) : BackgroundSessionHost {
  override fun start() = AirBeamForegroundService.start(context)
  override fun stop() = AirBeamForegroundService.stop(context)
}