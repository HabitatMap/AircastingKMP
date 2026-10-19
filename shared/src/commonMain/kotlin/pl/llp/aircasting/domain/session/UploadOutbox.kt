package pl.llp.aircasting.domain.session

import kotlinx.coroutines.flow.Flow

interface UploadOutbox {
  /** Oldest first, for one session — matching `POST /mobile_sessions/{uuid}/measurements`. */
  suspend fun pending(sessionUuid: String, limit: Int): List<Measurement>

  /**
   * The server accepted these in a `200` batch. Removes them from the outbox; the stored
   * measurements are untouched. A `200` is not proof of storage — the uploader must split
   * out frames past `finished_at` before sending.
   */
  suspend fun acknowledge(measurements: List<Measurement>)

  /** Uuids of mobile sessions with at least one row still to send. Re-emits on every change. */
  fun observeSessionsWithPending(): Flow<List<String>>
}