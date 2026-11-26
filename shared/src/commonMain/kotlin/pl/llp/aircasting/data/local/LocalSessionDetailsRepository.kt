package pl.llp.aircasting.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.active.SessionDetailsRepository
import pl.llp.aircasting.active.chipRank
import pl.llp.aircasting.data.local.db.SelectSessionStreamStats
import pl.llp.aircasting.data.local.db.SessionQueries
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.TrackPoint
import pl.llp.aircasting.data.local.db.Sessions
import pl.llp.aircasting.recording.PhoneMicDeviceId
import pl.llp.aircasting.ui.chart.ChartPoint
import pl.llp.aircasting.ui.chart.wallClockPoint
import kotlin.math.roundToInt

class LocalSessionDetailsRepository(
  private val queries: SessionQueries,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SessionDetailsRepository {

  /**
   * Two live queries: the session row (rename, finish) and its streams' stats (every measurement).
   * combine re-emits when either changes.
   */
  override fun observe(uuid: String): Flow<StationDetails?> = combine(
    queries.selectSessionByUuid(uuid).asFlow().mapToOneOrNull(ioDispatcher),
    queries.selectSessionStreamStats(uuid).asFlow().mapToList(ioDispatcher),
  ) { session, stats -> session?.toDetails(stats) }

  override fun observePoints(uuid: String, sensorName: String, timeZone: TimeZone): Flow<List<ChartPoint>> =
    queries.selectSessionStreamPoints(uuid, sensorName) { time, value -> wallClockPoint(time, value, timeZone) }
      .asFlow()
      .mapToList(ioDispatcher)

  override fun observeTrack(uuid: String, sensorName: String): Flow<List<TrackPoint>> =
    queries.selectSessionStreamTrack(uuid, sensorName) { latitude, longitude, value, time ->
      TrackPoint(GeoLocation(latitude, longitude), value, time)
    }
      .asFlow()
      .mapToList(ioDispatcher)
}

/** From the row, not the domain Session: mobile and fixed rows alike. */
private fun Sessions.toDetails(stats: List<SelectSessionStreamStats>) = StationDetails(
  uuid = uuid,
  type = type,
  // Same split as ActiveViewModel's two sections.
  source = if (device_id == PhoneMicDeviceId) DataSource.PhoneMic else DataSource.AirBeam,
  title = name,
  provider = "", // our own session: the screen shows deviceModel instead
  deviceModel = device_model,
  timeZone = TimeZone.of(time_zone),
  streams = stats.map { it.toStream() }.sortedBy { chipRank(it.sensorName) },
  tags = tags,
  isIndoor = is_indoor,
  location = latitude?.let { lat -> longitude?.let { lng -> GeoLocation(lat, lng) } },
  startTime = start_time,
  endTime = end_time,
)

private fun SelectSessionStreamStats.toStream() = StationStream(
  id = id,
  sensorName = sensor_name,
  unitSymbol = unit_symbol,
  value = now_value?.roundToInt(),
  min = min_value?.roundToInt(),
  max = max_value?.roundToInt(),
  measuredAt = now_time,
  avg = avg_value?.roundToInt(),
)