package com.lunarlogic.aircasting.di

import android.bluetooth.BluetoothManager
import android.content.Context
import org.koin.dsl.module

actual fun platformModule() = module {
  single { get<Context>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
}