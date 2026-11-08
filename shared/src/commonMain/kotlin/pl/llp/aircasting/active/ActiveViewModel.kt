package pl.llp.aircasting.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.llp.aircasting.domain.ThresholdsRepository

data class ActiveUiState(
  val mobile: List<ActiveSessionCard> = emptyList(),
  /** The card whose finish awaits confirmation (Figma 1136:18936); null = no dialog. */
  val confirmingFinish: ActiveSessionCard? = null,
)

class ActiveViewModel(
  sessions: ActiveSessionsRepository,
  thresholds: ThresholdsRepository,
  private val finish: suspend (uuid: String) -> Unit,
) : ViewModel() {
  /** Chosen chip per session uuid. Kept in memory only; legacy also persists it (SelectedStreams). */
  private val selectedSensors = MutableStateFlow(emptyMap<String, String>())
  private val confirming = MutableStateFlow<String?>(null)

  val state: StateFlow<ActiveUiState> = combine(
    sessions.observeMobile(),
    thresholds.observe(),
    selectedSensors,
    confirming,
  ) { mobile, levels, selected, confirmingUuid ->
    val cards = mobile.map { activeSessionCard(it.session, it.readings, levels, selected[it.session.uuid]) }
    ActiveUiState(mobile = cards, confirmingFinish = cards.firstOrNull { it.uuid == confirmingUuid })
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveUiState())

  fun selectStream(uuid: String, sensorName: String) = selectedSensors.update { it + (uuid to sensorName) }

  fun requestFinish(uuid: String) {
    confirming.value = uuid
  }

  fun dismissFinish() {
    confirming.value = null
  }

  fun confirmFinish() {
    val uuid = confirming.value ?: return
    confirming.value = null
    viewModelScope.launch { finish(uuid) }
  }
}