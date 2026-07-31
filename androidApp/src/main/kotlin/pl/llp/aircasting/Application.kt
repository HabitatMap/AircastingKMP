package pl.llp.aircasting

import android.app.Application
import pl.llp.aircasting.di.initKoin
import org.koin.android.ext.koin.androidContext

class AircastingApp : Application() {
  override fun onCreate() {
    super.onCreate()
    initKoin { androidContext(this@AircastingApp) }
  }
}