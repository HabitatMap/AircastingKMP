package com.lunarlogic.aircasting.home

import com.lunarlogic.aircasting.data.network.FixedStationsRepository
import com.lunarlogic.aircasting.domain.FixedStation
import com.lunarlogic.aircasting.domain.GeoLocation
import com.lunarlogic.aircasting.domain.GeoSquare
import com.lunarlogic.aircasting.domain.Pollutant
import com.lunarlogic.aircasting.domain.byDistanceFrom
import com.lunarlogic.aircasting.domain.mergeByStation
import com.lunarlogic.aircasting.domain.squareAround
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

interface HomeRepository {
  suspend fun load(userLocation: GeoLocation?): HomeUiState
}

class NetworkHomeRepository(
  private val stations: FixedStationsRepository,
  private val radiusKm: Double = 15.0,
): HomeRepository {
  override suspend fun load(userLocation: GeoLocation?): HomeUiState {
    if (userLocation == null) {
      println("AIRDIAG/REPO: userLocation is null -> NoLocation")
      return HomeUiState(HomeUiState.AirQuality.NoLocation, nearby = emptyList())
    }

    val box = userLocation.squareAround(radiusKm)
    println("AIRDIAG/REPO: box=$box (radiusKm=$radiusKm) around $userLocation")
    val fetched = fetchAll(box)
    fetched.forEach { (p, s) -> println("AIRDIAG/REPO: pollutant=$p -> ${s.size} stations") }
    val ranked = mergeByStation(fetched).byDistanceFrom(userLocation)
    println("AIRDIAG/REPO: merged+ranked = ${ranked.size} stations")

    val airQuality = ranked.firstOrNull()?.let { nearest ->
      HomeUiState.AirQuality.Loaded(
        stationName = nearest.station.name,
        distanceMeters = nearest.distanceMeters,
        readings = nearest.station.readings,
        updatedAt = nearest.station.updatedAt,
      )
    } ?: HomeUiState.AirQuality.NoReadings

    return HomeUiState(airQuality, nearby = ranked)
  }

  private suspend fun fetchAll(box: GeoSquare): Map<Pollutant, List<FixedStation>> =
    coroutineScope {
      Pollutant.entries
        .map { pollutant -> async { pollutant to stations.activeStations(box, pollutant) } }
        .awaitAll()
        .toMap()
    }
}