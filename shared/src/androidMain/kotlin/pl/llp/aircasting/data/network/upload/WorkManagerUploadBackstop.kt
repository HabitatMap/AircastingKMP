package pl.llp.aircasting.data.network.upload

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * One unique job that runs as soon as there is network, and is retried with WorkManager's
 * backoff until it succeeds — across process death and reboot. KEEP: a job already waiting
 * covers whatever was added since, because [MobileUploadLoop.drainOnce] reads the work fresh.
 */
class WorkManagerUploadBackstop(private val context: Context) : UploadBackstop {
  override fun schedule() {
    val request = OneTimeWorkRequestBuilder<UploadWorker>()
      .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
      .build()
    WorkManager.getInstance(context).enqueueUniqueWork(WorkName, ExistingWorkPolicy.KEEP, request)
  }

  private companion object {
    const val WorkName = "mobile-upload"
  }
}

/**
 * WorkManager constructs workers itself, so dependencies come from Koin rather than the
 * constructor. Koin is started in Application.onCreate, which always runs before a worker.
 */
class UploadWorker(context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params), KoinComponent {
  private val loop: MobileUploadLoop by inject()

  override suspend fun doWork(): Result = if (loop.drainOnce()) Result.success() else Result.retry()
}
