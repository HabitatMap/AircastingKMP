package pl.llp.aircasting.data.network

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.tokenOrNull
import pl.llp.aircasting.data.local.db.AppDatabase
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Mirrors the user's own fixed sessions from the backend into the DB; the screens observe the DB.
 * The backend is the only source of truth: nothing here is ever sent back. Signed out → no request.
 * Failures are logged only: the stored rows stay on screen until a later sync succeeds.
 */
class FixedSessionsSync(
  private val api: FixedSessionsApi,
  private val auth: AuthSession,
  private val db: AppDatabase,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  /**
   * `GET /fixed_sessions`: stores each session; one the backend reports finished (e.g. on the web) is
   * stored as finished, so its card leaves Active and it shows in Past Sessions through the DB flow.
   */
  suspend fun syncList() = withToken("fixed_sessions sync") { token ->
    // TEMPORARY until the backend offers an active-fixed-sessions endpoint (or a not-finished filter):
    //  then call that and drop the paging and this split. Until then: one page of the newest sessions.
    //  The full list (500) took 11 s for an account with 212 sessions, past the 10 s timeout, so an
    //  active session older than the newest 50 is missed. Legacy sessions all have finished_at null;
    //  the backend is to finish those itself.
    val sessions = api.listSessions(token, perPage = ONE_PAGE).sessions
    write { sessions.forEach { sync(it) } }
  }

  /** `GET /fixed_sessions/{uuid}`: refreshes one session (details opened); finished there → finished here. */
  suspend fun syncSession(uuid: String) = withToken("$uuid: sync") { token ->
    val session = api.getSession(token, uuid)
    write { sync(session) }
  }

  /**
   * One stream's history for the details graph: the 24 h before its newest stored reading (written by
   * [syncList]/[syncSession]), fetching only what came after the previous graph sync, then dropping
   * older readings. Legacy `DownloadMeasurementsService` + `LastMeasurementSyncCalculator` (since the
   * last sync, capped at 24 h) + `RemoveOldMeasurementsService` (keep 24 h). Uses its own cursor
   * (`synced_until`), not the newest reading: list syncs store the newest reading alone, past a gap.
   */
  suspend fun syncMeasurements(uuid: String, sensorName: String) = withToken("$uuid $sensorName: measurements") { token ->
    val (stream, newest) = withContext(ioDispatcher) {
      val stream = db.measurementStreamQueries.selectStream(uuid, sensorName).executeAsOneOrNull()
      stream to stream?.let { db.measurementQueries.selectLastTimeByStream(it.id).executeAsOneOrNull() }
    }
    if (stream == null || newest == null) return@withToken // never reported: nothing to anchor the 24 h on
    val from = newest - HISTORY
    val start = stream.synced_until?.let { it + 1.milliseconds }?.takeIf { it > from } ?: from
    val readings = if (start > newest) emptyList() else fetchMeasurements(token, uuid, sensorName, start, newest)
    write {
      readings.forEach {
        measurementQueries.insertMeasurement(stream.id, Instant.fromEpochMilliseconds(it.time), it.value, null, null)
      }
      measurementStreamQueries.updateSyncedUntil(newest, stream.id)
      measurementQueries.deleteOlderThan(stream.id, from)
    }
  }

  /** [start, end] in 12 h windows (the endpoint's max, both bounds inclusive), newest first, in parallel. */
  private suspend fun fetchMeasurements(
    token: String, uuid: String, sensorName: String, start: Instant, end: Instant,
  ): List<StationMeasurementDto> = coroutineScope {
    generateSequence(end) { it - WINDOW - 1.milliseconds }
      .takeWhile { it >= start }
      .map { windowEnd -> async { api.measurements(token, uuid, sensorName, maxOf(start, windowEnd - WINDOW), windowEnd) } }
      .toList()
      .awaitAll()
      .flatten()
  }

  /**
   * The user's Finish button: decommissions the session on the server. The answer's finished_at is the
   * only thing that finishes the row here (the backend is the source of truth for it).
   */
  suspend fun finish(uuid: String) = withToken("$uuid: finish") { token ->
    val finishedAt = api.finishSession(token, uuid).finishedAt ?: return@withToken
    write { fixedSessionQueries.finishFixed(Instant.fromEpochMilliseconds(finishedAt), uuid) }
  }

  /**
   * Never returns: deletes every stored fixed session whenever no one is signed in (also at launch), so
   * the next account never sees the previous one's. Legacy `LogoutService` clears the whole DB on logout;
   * mobile sessions are left alone here.
   */
  suspend fun clearWhenSignedOut() {
    auth.state.map { it.tokenOrNull == null }.distinctUntilChanged().filter { it }.collect {
      write { fixedSessionQueries.deleteAllFixed() }
    }
  }

  private suspend fun withToken(what: String, block: suspend (token: String) -> Unit) {
    val token = auth.state.value.tokenOrNull ?: return
    try {
      block(token)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      log.w(e) { "$what failed" }
    }
  }

  private suspend fun write(block: AppDatabase.() -> Unit) =
    withContext(ioDispatcher) { db.transaction { db.block() } }

  /**
   * The session row, its streams and each stream's newest reading, if any (what the Active card shows;
   * none yet → "No data yet"). The backend sets start_time at creation; the spec still marks it nullable,
   * so an entry without one is skipped rather than stored.
   */
  /**
   * Stores the session either way; one the backend reports finished goes straight to Past Sessions.
   * The backend's finished_at is the only thing that finishes a row (here or from [finish]). A null
   * finished_at never reopens a row: [store] doesn't touch status/end_time.
   */
  private fun AppDatabase.sync(dto: FixedSessionDto) {
    store(dto)
    val finishedAt = dto.finishedAt ?: return
    fixedSessionQueries.finishFixed(Instant.fromEpochMilliseconds(finishedAt), dto.uuid)
  }

  private fun AppDatabase.store(dto: FixedSessionDto) {
    val start = dto.startTime?.let(Instant::fromEpochMilliseconds) ?: run {
      log.w { "${dto.uuid}: no start_time; not stored" }
      return
    }
    val tags = dto.tagList.split(',', ' ').filter { it.isNotBlank() }
    // Indoor sessions carry the 200, 200 placeholder: no place to store.
    val latitude = dto.latitude.takeUnless { dto.isIndoor }
    val longitude = dto.longitude.takeUnless { dto.isIndoor }
    val reported = dto.lastMeasurementAt?.let(Instant::fromEpochMilliseconds)
    fixedSessionQueries.insertFixedIfAbsent(
      dto.uuid, dto.title, tags, dto.device?.model, start, latitude, longitude, dto.isIndoor, dto.timeZone,
      reported,
    )
    fixedSessionQueries.updateFixed(
      dto.title, tags, dto.device?.model, start, latitude, longitude, dto.isIndoor, dto.timeZone, reported,
      dto.uuid,
    )
    dto.streams.values.forEach { stream ->
      measurementStreamQueries.insertStreamIfAbsent(dto.uuid, stream.sensorName, stream.unitSymbol, null)
      val last = stream.lastMeasurement ?: return@forEach
      val streamId = measurementStreamQueries.selectStream(dto.uuid, stream.sensorName).executeAsOne().id
      measurementQueries.insertMeasurement(streamId, Instant.fromEpochMilliseconds(last.time), last.value, null, null)
    }
  }

  private companion object {
    const val ONE_PAGE = 50
    val HISTORY = 24.hours
    val WINDOW = 12.hours
    val log = Logger.withTag("FixedSessionsSync")
  }
}
