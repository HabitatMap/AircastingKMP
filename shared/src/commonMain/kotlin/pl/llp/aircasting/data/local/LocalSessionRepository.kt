package pl.llp.aircasting.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pl.llp.aircasting.data.local.db.SessionQueries
import pl.llp.aircasting.data.local.db.Sessions
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.session.Session
import pl.llp.aircasting.domain.session.SessionRepository
import pl.llp.aircasting.domain.session.SessionStatus
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class LocalSessionRepository(
  private val queries: SessionQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SessionRepository {

  override suspend fun insert(session: Session): Unit = withContext(ioDispatcher) {
    queries.insertSession(
      uuid = session.uuid,
      type = session.type,
      status = session.status,
      name = session.name,
      tags = session.tags,
      device_id = session.deviceId,
      device_model = session.deviceModel,
      start_time = session.startTime,
      end_time = session.endTime,
      latitude = session.location?.latitude,
      longitude = session.location?.longitude,
      is_indoor = session.isIndoor,
      is_contributing_to_crowd_map = session.isContributingToCrowdMap,
      time_zone = session.timeZone,
      measurement_interval_millis = session.measurementInterval.inWholeMilliseconds,
      session_token = session.sessionToken,
    )
  }

  override suspend fun find(uuid: String): Session? = withContext(ioDispatcher) {
    queries.selectSessionByUuid(uuid).executeAsOneOrNull()?.toDomain()
  }
  override fun observe(uuid: String): Flow<Session?> =
    queries.selectSessionByUuid(uuid)
      .asFlow()
      .mapToOneOrNull(ioDispatcher)
      .map { it?.toDomain() }

  override fun observeAll(): Flow<List<Session>> =
    queries.selectAllSessions()
      .asFlow()
      .mapToList(ioDispatcher)
      .map { rows -> rows.map { it.toDomain() } }

  override suspend fun updateStatus(uuid: String, status: SessionStatus): Unit =
    withContext(ioDispatcher) { queries.updateSessionStatus(status, uuid) }

  override suspend fun finish(uuid: String, endTime: Instant): Unit = withContext(ioDispatcher) {
    queries.finishSession(status = SessionStatus.FINISHED, endTime = endTime, uuid = uuid)
  }

  override suspend fun markFinishedOnServer(uuid: String): Unit =
    withContext(ioDispatcher) { queries.markFinishedOnServer(uuid) }

  override suspend fun delete(uuid: String): Unit =
    withContext(ioDispatcher) { queries.deleteSession(uuid) }
}

private fun Sessions.toDomain() = Session(
  uuid = uuid,
  type = type,
  status = status,
  name = name,
  tags = tags,
  startTime = start_time,
  timeZone = time_zone,
  measurementInterval = measurement_interval_millis.milliseconds,
  endTime = end_time,
  deviceId = device_id,
  deviceModel = device_model,
  location = latitude?.let { lat -> longitude?.let { lon -> GeoLocation(lat, lon) } },
  isIndoor = is_indoor,
  isContributingToCrowdMap = is_contributing_to_crowd_map,
  sessionToken = session_token,
  isFinishedOnServer = is_finished_on_server,
)