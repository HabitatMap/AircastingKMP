package pl.llp.aircasting.data.network.upload

/**
 * The app is going to background: make sure pending uploads still go out if the process is
 * about to be killed or suspended. Android: a WorkManager job; iOS: a background task.
 */
fun interface UploadBackstop {
  fun schedule()
}
