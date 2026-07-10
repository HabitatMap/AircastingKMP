package com.lunarlogic.aircasting

import android.app.Application
import com.lunarlogic.aircasting.di.initKoin
import org.koin.android.ext.koin.androidContext

class AircastingApp : Application() {
  override fun onCreate() {
    super.onCreate()
    initKoin { androidContext(this@AircastingApp) }
  }
}