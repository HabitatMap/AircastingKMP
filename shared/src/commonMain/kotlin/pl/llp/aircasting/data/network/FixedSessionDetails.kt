package pl.llp.aircasting.data.network

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.active.chipRank
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.tokenOrNull
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.explore.NetworkStationMeasurementsRepository
import pl.llp.aircasting.explore.StationDetailsRepository
import pl.llp.aircasting.explore.StationMeasurementsRepository
import kotlin.math.roundToInt
import kotlin.time.Instant

/**
 * Our own fixed session for the shared details screen. Not Explore's `/explore/stations/{uuid}`: that
 * one only knows public outdoor stations, while ours may be indoor; this one needs the user token.
 */
class NetworkFixedSessionDetailsRepository(
  private val api: FixedSessionsApi,
  private val auth: AuthSession,
) : StationDetailsRepository {
  override suspend fun station(uuid: String): StationDetails = api.getSession(auth.requireToken(), uuid).toStationDetails()
}

/** The details graph for our own fixed session: Explore's 24 h windows, read from `/fixed_sessions`. */
fun fixedSessionMeasurements(api: FixedSessionsApi, auth: AuthSession): StationMeasurementsRepository =
  NetworkStationMeasurementsRepository { uuid, sensorName, start, end ->
    api.measurements(auth.requireToken(), uuid, sensorName, start, end)
  }

/** Throws when signed out: the details screen shows its Error state with Retry. */
private fun AuthSession.requireToken(): String = state.value.tokenOrNull ?: error("Signed out")

internal fun FixedSessionDto.toStationDetails() = StationDetails(
  uuid = uuid,
  type = SessionType.FIXED,
  source = DataSource.AirBeam,
  title = title,
  provider = "", // our own session: the screen shows deviceModel instead
  deviceModel = device?.model,
  timeZone = TimeZone.of(timeZone),
  // Keyed by name on the wire, so no stable server id: the chip-order index is unique per session.
  streams = streams.values.sortedBy { chipRank(it.sensorName) }.mapIndexed { index, stream ->
    StationStream(
      id = index.toLong(),
      sensorName = stream.sensorName,
      unitSymbol = stream.unitSymbol,
      value = stream.lastMeasurement?.value?.roundToInt(),
      min = null,
      max = null,
      measuredAt = stream.lastMeasurement?.time?.let(Instant::fromEpochMilliseconds),
    )
  },
  tags = tagList.split(',', ' ').filter { it.isNotBlank() },
  isIndoor = isIndoor,
  // Indoor sessions carry the 200, 200 placeholder: nothing to put on a map.
  location = if (isIndoor) null else latitude?.let { lat -> longitude?.let { lng -> GeoLocation(lat, lng) } },
  startTime = startTime?.let(Instant::fromEpochMilliseconds),
  endTime = finishedAt?.let(Instant::fromEpochMilliseconds),
)
