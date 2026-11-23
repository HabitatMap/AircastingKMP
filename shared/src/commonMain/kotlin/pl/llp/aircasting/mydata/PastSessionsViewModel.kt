package pl.llp.aircasting.mydata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import pl.llp.aircasting.domain.ThresholdsRepository

data class PastSessionsUiState(val sessions: List<PastSessionCard> = emptyList())

class PastSessionsViewModel(
  sessions: PastSessionsRepository,
  thresholds: ThresholdsRepository,
) : ViewModel() {
  /** Chosen chip per session uuid, in memory only — same as ActiveViewModel. */
  private val selectedSensors = MutableStateFlow(emptyMap<String, String>())

  val state: StateFlow<PastSessionsUiState> = combine(
    sessions.observe(),
    thresholds.observe(),
    selectedSensors,
  ) { all, levels, selected ->
    PastSessionsUiState(all.map { pastSessionCard(it, levels, selected[it.session.uuid]) })
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PastSessionsUiState())

  fun selectStream(uuid: String, sensorName: String) = selectedSensors.update { it + (uuid to sensorName) }
}
