package pl.llp.aircasting.data.network.upload

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Virtual time: `advanceTimeBy(d)` runs everything scheduled strictly before now + d, and
 * `runCurrent()` runs what is due exactly now. So "nothing at 59 s, a drain at 60 s" is
 * `advanceTimeBy(59.seconds)` → assert → `advanceTimeBy(1.seconds); runCurrent()` → assert.
 */
class MobileUploadLoopTest {

  private val pending = MutableStateFlow(emptyList<String>())
  private val awaitingFinish = MutableStateFlow(emptyList<String>())
  private val uploader = FakeUploader(pending, awaitingFinish)
  private val loop = MobileUploadLoop(
    sessionsWithPending = pending,
    sessionsAwaitingFinish = awaitingFinish,
    uploader = uploader,
    flushInterval = 60.seconds,
    initialBackoff = 30.seconds,
    maxBackoff = 10.minutes,
  )

  /** Rule 3: rows of a still-recording session are batched — one drain per interval. */
  @Test
  fun rows_of_a_recording_session_wait_the_flush_interval() = runTest {
    start()
    pending.value = listOf(A)

    advanceBy(59.seconds)
    assertEquals(emptyList(), uploader.uploads)

    advanceBy(1.seconds)
    assertEquals(listOf(A), uploader.uploads)
  }

  /** Recording emits new rows every second; those must not keep pushing the drain back. */
  @Test
  fun new_rows_during_the_wait_do_not_postpone_the_drain() = runTest {
    start()
    pending.value = listOf(A)
    advanceBy(30.seconds)
    pending.value = listOf(A, B)

    advanceBy(30.seconds)
    assertEquals(setOf(A, B), uploader.uploads.toSet())
  }

  /** Rule 2: stopping is itself the trigger — no wait, no call from the stop flow. */
  @Test
  fun a_stopped_session_drains_at_once() = runTest {
    start()
    awaitingFinish.value = listOf(A)
    runCurrent()

    assertEquals(listOf(A), uploader.uploads)
  }

  /** A stop in the middle of a batching wait cuts the wait short. */
  @Test
  fun stopping_during_the_wait_drains_at_once() = runTest {
    start()
    pending.value = listOf(A)
    advanceBy(10.seconds)

    awaitingFinish.value = listOf(A)
    runCurrent()

    assertEquals(listOf(A), uploader.uploads)
  }

  /** Rule 1: no work, no timer. */
  @Test
  fun with_nothing_to_send_the_loop_stays_idle() = runTest {
    start()
    advanceBy(1.hours)

    assertEquals(emptyList(), uploader.uploads)
  }

  /**
   * Rule 4: a failure backs off, doubling — and the backoff wins over rule 2, otherwise a
   * session that keeps failing its /finish would be retried in a tight loop.
   */
  @Test
  fun a_failure_backs_off_doubling() = runTest {
    start()
    uploader.failing += A
    awaitingFinish.value = listOf(A)
    runCurrent()
    assertEquals(1, uploader.uploads.size)

    advanceBy(29.seconds)
    assertEquals(1, uploader.uploads.size)
    advanceBy(1.seconds)
    assertEquals(2, uploader.uploads.size)

    advanceBy(59.seconds)
    assertEquals(2, uploader.uploads.size)
    advanceBy(1.seconds)
    assertEquals(3, uploader.uploads.size)
  }

  @Test
  fun a_success_resets_the_backoff() = runTest {
    start()
    uploader.failing += A
    awaitingFinish.value = listOf(A)
    runCurrent()                       // fails at 0 s
    advanceBy(30.seconds)              // fails at 30 s, next try in 60 s
    uploader.failing.clear()
    advanceBy(60.seconds)              // succeeds at 90 s
    assertEquals(3, uploader.uploads.size)

    uploader.failing += A
    awaitingFinish.value = listOf(A)
    runCurrent()                       // fails again
    advanceBy(30.seconds)              // retried after 30 s again, not 120 s

    assertEquals(5, uploader.uploads.size)
  }

  /** One broken session must not hold back the others in the same pass. */
  @Test
  fun one_failing_session_does_not_stop_the_others() = runTest {
    start()
    uploader.failing += A
    awaitingFinish.value = listOf(A, B)
    runCurrent()

    assertEquals(listOf(A, B), uploader.uploads)
  }

  /** The Android worker's single pass: everything now, no batching wait. */
  @Test
  fun drainOnce_sends_everything_at_once_and_reports_success() = runTest {
    pending.value = listOf(A)
    awaitingFinish.value = listOf(B)

    assertTrue(loop.drainOnce())
    assertEquals(setOf(A, B), uploader.uploads.toSet())
  }

  /** false → the worker returns Result.retry() and WorkManager applies its own backoff. */
  @Test
  fun drainOnce_reports_a_failure_after_trying_every_session() = runTest {
    uploader.failing += A
    pending.value = listOf(A, B)

    assertFalse(loop.drainOnce())
    assertEquals(listOf(A, B), uploader.uploads)
  }

  @Test
  fun drainOnce_with_nothing_to_send_succeeds_without_uploading() = runTest {
    assertTrue(loop.drainOnce())
    assertEquals(emptyList(), uploader.uploads)
  }

  /**
   * Worker and loop share one process and may fire together. Running side by side, both would
   * read the same outbox rows and POST them twice (and /finish twice). They must take turns, and
   * the second must read the work only after the first finished.
   */
  @Test
  fun drainOnce_waits_for_a_running_drain_and_then_sees_its_result() = runTest {
    start()
    val gate = CompletableDeferred<Unit>()
    uploader.gate = gate
    awaitingFinish.value = listOf(A)
    runCurrent()                                   // the loop is mid-upload of A
    assertEquals(listOf(A), uploader.uploads)

    val second = async { loop.drainOnce() }
    runCurrent()
    assertEquals(listOf(A), uploader.uploads)      // waiting its turn

    gate.complete(Unit)
    runCurrent()
    assertTrue(second.await())
    assertEquals(listOf(A), uploader.uploads)      // A is done; not sent twice
  }

  private fun TestScope.start() {
    backgroundScope.launch { loop.run() }
    runCurrent()
  }

  private fun TestScope.advanceBy(duration: Duration) {
    advanceTimeBy(duration)
    runCurrent()
  }

  /**
   * Records every call. A success does what the real one does to the database — the outbox
   * empties and the server has the end time — so the two lists stop reporting the session.
   */
  private class FakeUploader(
    private val pending: MutableStateFlow<List<String>>,
    private val awaitingFinish: MutableStateFlow<List<String>>,
  ) : SessionUploader {
    val uploads = mutableListOf<String>()
    val failing = mutableSetOf<String>()
    /** When set, every upload suspends here until it completes — "a request in flight". */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun upload(sessionUuid: String) {
      uploads += sessionUuid
      gate?.await()
      if (sessionUuid in failing) error("offline")
      pending.value -= sessionUuid
      awaitingFinish.value -= sessionUuid
    }
  }

  private companion object {
    const val A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    const val B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
  }
}
