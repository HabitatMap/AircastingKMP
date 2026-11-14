package pl.llp.aircasting.data.network

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.ExploreSource
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.session.SessionType
import kotlin.time.Instant

/** Unlike the map (skip unknown items), the user tapped this one station: an unknown source is an error. */
fun StationDetailsDto.toStationDetails(): StationDetails = StationDetails(
  uuid = uuid,
  type = SessionType.FIXED,
  source = when (ExploreSource.entries.find { it.apiName == source } ?: error("Unknown source '$source'")) {
    ExploreSource.AirBeam -> DataSource.AirBeam
    ExploreSource.Government -> DataSource.Government
  },
  title = title,
  provider = provider,
  deviceModel = deviceModel,
  timeZone = TimeZone.of(timeZone),
  streams = streams.map {
    StationStream(
      id = it.streamId,
      sensorName = it.sensorName,
      unitSymbol = it.unitSymbol,
      value = it.lastMeasurement?.value,
      min = it.last24h?.min,
      max = it.last24h?.max,
      measuredAt = it.lastMeasurement?.time?.let(Instant::fromEpochMilliseconds),
    )
  },
)