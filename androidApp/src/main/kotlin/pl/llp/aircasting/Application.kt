package pl.llp.aircasting

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import pl.llp.aircasting.data.network.upload.UploadBackstop
import pl.llp.aircasting.di.initKoin

class AircastingApp : Application() {
  override fun onCreate() {
    super.onCreate()
    initKoin { androidContext(this@AircastingApp) }
    // Last activity stopped: the process may be killed soon, so hand pending uploads to WorkManager.
    ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onStop(owner: LifecycleOwner) = get<UploadBackstop>().schedule()
    })
  }
}
