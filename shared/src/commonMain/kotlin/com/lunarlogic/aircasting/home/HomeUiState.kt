package com.lunarlogic.aircasting.home

import com.lunarlogic.aircasting.domain.PollutantReading
import kotlin.time.Instant

sealed interface HomeUiState {
  data object NoLocation : HomeUiState

  data object NoReadings : HomeUiState

  data class Loaded(
    val stationName: String,
    val distanceMeters: Double,
    val readings: List<PollutantReading>,
    val updatedAt: Instant,
  ) : HomeUiState
}