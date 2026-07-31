package pl.llp.aircasting.home

import pl.llp.aircasting.domain.PollutantReading
import pl.llp.aircasting.domain.StationWithDistance
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
