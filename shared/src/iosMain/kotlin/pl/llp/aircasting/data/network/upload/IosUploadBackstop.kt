package pl.llp.aircasting.data.network.upload

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskInvalid

/**
 * iOS has no WorkManager: the best it offers is ~30 s of extra run time after backgrounding.
 * One drain runs inside that window. Whatever doesn't make it waits in SQLite for the next
 * launch or foreground — delayed, never lost.
 */
class IosUploadBackstop(private val loop: MobileUploadLoop) : UploadBackstop {
  private val scope = MainScope()

  override fun schedule() {
    val app = UIApplication.sharedApplication
    var taskId = UIBackgroundTaskInvalid
    // Everything here runs on the main thread, so the check-and-clear needs no lock.
    fun end() {
      if (taskId == UIBackgroundTaskInvalid) return
      app.endBackgroundTask(taskId)
      taskId = UIBackgroundTaskInvalid
    }

    val drain = scope.launch(start = CoroutineStart.LAZY) {
      try {
        loop.drainOnce()
      } finally {
        end()
      }
    }
    // iOS kills the app unless the task is ended synchronously inside the expiration handler.
    taskId = app.beginBackgroundTaskWithName("mobile-upload") {
      drain.cancel()
      end()
    }
    drain.start()
  }
}
