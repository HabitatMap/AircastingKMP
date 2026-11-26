package pl.llp.aircasting.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.llp.aircasting.domain.ThresholdsRepository
import pl.llp.aircasting.recording.PhoneMicDeviceId
import kotlin.time.Clock

data class ActiveUiState(
  val mobile: List<ActiveSessionCard> = emptyList(),
  /** Mirrored from the backend into the DB (`FixedSessionsSync`); the backend is their source of truth. */
  val fixed: List<ActiveSessionCard> = emptyList(),
  /** Phone-mic sessions: also MOBILE rows, told apart by [PhoneMicDeviceId]. */
  val microphone: List<ActiveSessionCard> = emptyList(),
  val confirmingFinish: ActiveSessionCard? = null,
)

class ActiveViewModel(
  sessions: ActiveSessionsRepository,
  private val fixedSessions: ActiveFixedSessionsRepository,
  thresholds: ThresholdsRepository,
  /** Judges "Offline" on each emission; the fixed list re-emits every poll, so it stays current. */
  private val clock: Clock,
  private val finish: suspend (uuid: String) -> Unit,
) : ViewModel() {
  /** Chosen chip per session uuid. Kept in memory only; legacy also persists it (SelectedStreams). */
  private val selectedSensors = MutableStateFlow(emptyMap<String, String>())
  private val confirming = MutableStateFlow<String?>(null)

  val state: StateFlow<ActiveUiState> = combine(
    sessions.observeMobile(),
    // Starts empty: combine waits for every source's first value, and mobile cards must not wait on
    // the fixed source (a fake or a slow first DB read).
    fixedSessions.observeFixed().onStart { emit(emptyList()) },
    thresholds.observe(),
    selectedSensors,
    confirming,
  ) { all, fixed, levels, selected, confirmingUuid ->
    fun List<ActiveMobileSession>.cards() =
      map { activeSessionCard(it.session, it.readings, levels, selected[it.session.uuid]) }
    val (mic, airBeam) = all.partition { it.session.deviceId == PhoneMicDeviceId }
    val mobile = airBeam.cards()
    val microphone = mic.cards()
    val now = clock.now()
    val fixedCards = fixed.map { activeFixedSessionCard(it, levels, selected[it.uuid], now) }
    ActiveUiState(
      mobile = mobile,
      fixed = fixedCards,
      microphone = microphone,
      confirmingFinish = (mobile + fixedCards + microphone).firstOrNull { it.uuid == confirmingUuid },
    )
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveUiState())


  fun selectStream(uuid: String, sensorName: String) = selectedSensors.update { it + (uuid to sensorName) }

  /** The Offline card's Reconnect: re-config over BLE isn't built, so it checks the server now. */
  fun reconnect() = fixedSessions.refresh()

  fun requestFinish(uuid: String) {
    confirming.value = uuid
  }

  fun dismissFinish() {
    confirming.value = null
  }

  fun confirmFinish() {
    val uuid = confirming.value ?: return
    confirming.value = null
    // Fixed sessions run on the AirBeam and the server: nothing local to stop.
    val isFixed = state.value.fixed.any { it.uuid == uuid }
    viewModelScope.launch { if (isFixed) fixedSessions.finish(uuid) else finish(uuid) }
  }
}