package pl.llp.aircasting.data.network

import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import kotlin.time.Instant

/** Unlike the map (skip unknown items), the user tapped this one station: an unknown source is an error. */
fun StationDetailsDto.toStationDetails(): StationDetails = StationDetails(
  uuid = uuid,
  source = DataSource.entries.find { it.apiName == source } ?: error("Unknown source '$source'"),
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
      min24h = it.last24h?.min,
      max24h = it.last24h?.max,
      measuredAt = it.lastMeasurement?.time?.let(Instant::fromEpochMilliseconds),
    )
  },
)