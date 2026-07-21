package com.lunarlogic.aircasting.home

import com.lunarlogic.aircasting.domain.PollutantReading
import com.lunarlogic.aircasting.domain.StationWithDistance
import kotlin.time.Instant

data class HomeUiState(
  val airQuality: AirQuality,
  val nearby: List<StationWithDistance>,
) {
  sealed interface AirQuality {
    data object NoLocation : AirQuality

    data object NoReadings : AirQuality

    data class Loaded(
      val stationName: String,
      val distanceMeters: Double,
      val readings: List<PollutantReading>,
      val updatedAt: Instant,
    ) : AirQuality
  }
}
