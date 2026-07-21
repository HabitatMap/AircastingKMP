package com.lunarlogic.aircasting.home

import com.lunarlogic.aircasting.data.network.FixedStationsRepository
import com.lunarlogic.aircasting.domain.GeoLocation
import com.lunarlogic.aircasting.domain.Pollutant
import com.lunarlogic.aircasting.domain.mergeByStation
import com.lunarlogic.aircasting.domain.nearestTo
import com.lunarlogic.aircasting.domain.squareAround
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class HomeRepository(
  private val stations: FixedStationsRepository,
) {
  suspend fun load(userLocation: GeoLocation?): HomeUiState {
    if (userLocation == null) return HomeUiState.NoLocation

    val box = userLocation.squareAround()
    val byPollutant = coroutineScope {
      Pollutant.entries
        .map { pollutant -> async { pollutant to stations.activeStations(box, pollutant) } }
        .awaitAll()
        .toMap()
    }

    val nearest = mergeByStation(byPollutant).nearestTo(userLocation)
      ?: return HomeUiState.NoReadings

    return HomeUiState.Loaded(
      stationName = nearest.station.name,
      distanceMeters = nearest.distanceMeters,
      readings = nearest.station.readings,
      updatedAt = nearest.station.updatedAt,
    )
  }
}