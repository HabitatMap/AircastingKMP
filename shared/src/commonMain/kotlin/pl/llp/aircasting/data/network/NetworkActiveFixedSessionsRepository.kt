package pl.llp.aircasting.data.network

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import pl.llp.aircasting.active.ActiveFixedSession
import pl.llp.aircasting.active.ActiveFixedSessionsRepository
import pl.llp.aircasting.active.LatestReading
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.tokenOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Polls `GET /api/v3/fixed_sessions` while collected. The AirBeam uploads about once a minute, so
 * polling faster shows nothing new. Follows the signed-in account: sign-out → empty, no request.
 */
class NetworkActiveFixedSessionsRepository(
  private val api: FixedSessionsApi,
  private val auth: AuthSession,
  private val pollEvery: Duration = 60.seconds,
) : ActiveFixedSessionsRepository {
  /** Finished by the user here, per the server's answer: hidden until the next fetch agrees. */
  private val finished = MutableStateFlow(emptySet<String>())

  /** DROP_OLDEST: taps while a fetch runs collapse into one more fetch. */
  private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun observeFixed(): Flow<List<ActiveFixedSession>> =
    auth.state.map { it.tokenOrNull }.distinctUntilChanged().flatMapLatest { token ->
      if (token == null) flowOf(emptyList())
      else merge(ticks(), refreshes).onStart { emit(Unit) }.mapNotNull { fetch(token) }
    }
      .combine(finished) { sessions, done -> sessions.filterNot { it.uuid in done } }

  override fun refresh() {
    refreshes.tryEmit(Unit)
  }

  override suspend fun finish(uuid: String) {
    val token = auth.state.value.tokenOrNull ?: return
    try {
      // The answer carries finished_at: no need to re-fetch the list to drop the card.
      if (api.finishSession(token, uuid).finishedAt != null) finished.update { it + uuid }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // The card stays; the user can tap Finish again.
      log.w(e) { "$uuid: finish failed" }
    }
  }

  private fun ticks() = flow {
    while (true) {
      delay(pollEvery)
      emit(Unit)
    }
  }

  /** Null on failure: emits nothing, so the last good list stays on screen until the next fetch. */
  private suspend fun fetch(token: String): List<ActiveFixedSession>? = try {
    // TODO: replace with the backend's dedicated active-fixed-sessions endpoint once it exists, and
    //  drop this paging and the finished_at filter. Until then: one page of the newest sessions. The
    //  full list (500) took 11 s for an account with 212 sessions, past the 10 s timeout, and every
    //  legacy session has finished_at null, so an active session older than the newest 50 is missed.
    api.listSessions(token, perPage = ONE_PAGE).sessions
      .filter { it.finishedAt == null }
      .map { it.toActiveFixedSession() }
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    log.w(e) { "fixed_sessions fetch failed" }
    null
  }

  private companion object {
    const val ONE_PAGE = 50
    val log = Logger.withTag("ActiveFixedSessions")
  }
}

internal fun FixedSessionDto.toActiveFixedSession() = ActiveFixedSession(
  uuid = uuid,
  name = title,
  tags = tagList.split(',', ' ').filter { it.isNotBlank() },
  deviceModel = device?.model,
  isIndoor = isIndoor,
  timeZone = timeZone,
  startTime = startTime?.let(Instant::fromEpochMilliseconds),
  lastReportedAt = lastMeasurementAt?.let(Instant::fromEpochMilliseconds),
  readings = streams.values.map {
    LatestReading(
      sensorName = it.sensorName,
      unitSymbol = it.unitSymbol,
      value = it.lastMeasurement?.value,
      time = it.lastMeasurement?.time?.let(Instant::fromEpochMilliseconds),
    )
  },
)
