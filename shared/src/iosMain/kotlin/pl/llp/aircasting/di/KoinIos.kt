package pl.llp.aircasting.di

import org.koin.mp.KoinPlatform
import pl.llp.aircasting.data.network.upload.UploadBackstop

fun startKoinIos() = initKoin()

/** Called from Swift when the scene goes to background. */
fun scheduleUploadBackstop() = KoinPlatform.getKoin().get<UploadBackstop>().schedule()
