package pl.llp.aircasting

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pl.llp.aircasting.data.network.upload.UploadBackstop
import pl.llp.aircasting.di.initKoin
import pl.llp.aircasting.recording.BackgroundSessionHost
import pl.llp.aircasting.service.ForegroundServiceSessionHost

class AircastingApp : Application() {
  override fun onCreate() {
    super.onCreate()
    initKoin {
      androidContext(this@AircastingApp)
      modules(module { single<BackgroundSessionHost> { ForegroundServiceSessionHost(get()) } })
    }
    // Last activity stopped: the process may be killed soon, so hand pending uploads to WorkManager.
    ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onStop(owner: LifecycleOwner) = get<UploadBackstop>().schedule()
    })
  }
}
