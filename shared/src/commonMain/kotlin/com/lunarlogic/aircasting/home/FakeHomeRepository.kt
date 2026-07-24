package com.lunarlogic.aircasting.home

import com.lunarlogic.aircasting.domain.GeoLocation
import com.lunarlogic.aircasting.domain.MeasurementLevel
import com.lunarlogic.aircasting.domain.NearbyStation
import com.lunarlogic.aircasting.domain.Pollutant
import com.lunarlogic.aircasting.domain.PollutantReading
import com.lunarlogic.aircasting.domain.StationWithDistance
import kotlin.time.Clock

class FakeHomeRepository : HomeRepository {
  override suspend fun load(userLocation: GeoLocation?): HomeUiState {
    val now = Clock.System.now()

    fun reading(p: Pollutant, value: Double) =
      PollutantReading(p, value, p.unitSymbol, MeasurementLevel.LOW) // LOW ⇒ "Good"

    val downtown = NearbyStation(
      name = "Downtown Sensor",
      location = GeoLocation(40.7128, -74.0060),
      isIndoor = false,
      readings = listOf(
        reading(Pollutant.PM25, 4.2),
        reading(Pollutant.NO2, 9.1),
        reading(
          Pollutant.OZONE, 12.0
        )
      ),
      updatedAt = now,
    )
    val riverside = NearbyStation(
      name = "Riverside Park",
      location = GeoLocation(40.7180, -73.9910),
      isIndoor = false,
      readings = listOf(
        reading(Pollutant.PM25, 6.5),
        reading(Pollutant.OZONE, 14.0),
      ),
      updatedAt = now,
    )
    val office = NearbyStation(
      name = "Office Indoor",
      location = GeoLocation(40.7090, -74.0120),
      isIndoor = true,
      readings = listOf(reading(Pollutant.PM25, 3.1)),
      updatedAt = now,
    )
    return HomeUiState(
      airQuality = HomeUiState.AirQuality.Loaded(
        stationName = downtown.name,
        distanceMeters = 320.0,
        readings = downtown.readings, // PM25 + NO2 + Ozone, all LOW ⇒ "Good"
        updatedAt = now,
      ),
      nearby = listOf(
        StationWithDistance(downtown, 320.0),
        StationWithDistance(riverside, 1450.0),
        StationWithDistance(office, 210.0),
      ),
    )
  }
}