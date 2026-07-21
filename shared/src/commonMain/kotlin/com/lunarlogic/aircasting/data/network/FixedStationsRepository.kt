package com.lunarlogic.aircasting.data.network

import com.lunarlogic.aircasting.domain.FixedStation
import com.lunarlogic.aircasting.domain.GeoSquare
import com.lunarlogic.aircasting.domain.Pollutant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class FixedStationsRepository(
  private val api: FixedStationsApi,
  private val clock: Clock,
) {
  suspend fun activeStations(area: GeoSquare, pollutant: Pollutant): List<FixedStation> {
    val now = clock.now()
    val query = FixedStationsQuery(
      timeFrom = (now - 365.days).epochSeconds.toString(),
      timeTo = now.epochSeconds.toString(),
      west = area.west, east = area.east, south = area.south, north = area.north,
      sensorName = pollutant.sensorName,
      unitSymbol = pollutant.unitSymbol,
      measurementType = pollutant.measurementType,
    )
    return api.activeInRegion(query).sessions.map { it.toFixedStation() }
  }
}