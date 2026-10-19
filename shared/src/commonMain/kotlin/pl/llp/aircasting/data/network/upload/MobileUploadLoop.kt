package pl.llp.aircasting.data.network.upload

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps mobile sessions uploaded while the process lives. Driven by local data, not events:
 * pending rows wait [flushInterval] to batch; a stopped session not yet finished on the
 * server goes at once; a failed pass backs off, doubling up to [maxBackoff].
 */
class MobileUploadLoop(
  private val sessionsWithPending: Flow<List<String>>,
  private val sessionsAwaitingFinish: Flow<List<String>>,
  private val uploader: SessionUploader,
  private val flushInterval: Duration = 60.seconds,
  private val initialBackoff: Duration = 30.seconds,
  private val maxBackoff: Duration = 10.minutes,
) {
  private val log = Logger.withTag("UploadLoop")
  private val flushRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  /** Cuts the current batching wait short. Without a wait in progress there's nothing to cut. */
  fun flushNow() {
    flushRequests.tryEmit(Unit)
  }

  /** Never returns; cancel the calling scope to stop. */
  suspend fun run() = coroutineScope {
    val work = combine(sessionsWithPending, sessionsAwaitingFinish, ::Work).stateIn(this)
    var backoff: Duration? = null

    while (true) {
      work.first { it.all.isNotEmpty() }
      if (backoff == null && work.value.awaitingFinish.isEmpty()) waitToBatch(work)

      val snapshot = work.value
      val failed = drain(snapshot.all)
      backoff = if (failed) backoff?.times(2)?.coerceAtMost(maxBackoff) ?: initialBackoff else null
      if (backoff != null) delay(backoff)
      // The drain's writes reach the queries asynchronously; don't re-drain the stale snapshot.
      else work.first { it !== snapshot }
    }
  }
  private suspend fun waitToBatch(work: StateFlow<Work>) {
    withTimeoutOrNull(flushInterval) {
      merge(flushRequests, work.filter { it.awaitingFinish.isNotEmpty() }.map { }).first()
    }
  }

  /** @return true if any session failed; the others still went. */
  private suspend fun drain(uuids: List<String>): Boolean {
    var failed = false
    for (uuid in uuids) {
      try {
        uploader.upload(uuid)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        failed = true
        log.w(e) { "$uuid: upload failed, will retry" }
      }
    }
    return failed
  }
  private class Work(val pending: List<String>, val awaitingFinish: List<String>) {
    val all = (awaitingFinish + pending).distinct()
  }
}