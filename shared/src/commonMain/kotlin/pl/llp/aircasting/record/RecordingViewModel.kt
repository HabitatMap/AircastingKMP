package pl.llp.aircasting.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.LinkState
import pl.llp.aircasting.recording.NewSessionDetails
import pl.llp.aircasting.recording.RecordingEngine

sealed interface StartState {
  data object Idle : StartState
  data object Starting : StartState
  data object Started : StartState
  data class Failed(val result: ConfigResult) : StartState
}

/** The UI's handle on [RecordingEngine]: the wizard starts a session, the shell stops it. */
class RecordingViewModel(
  private val link: AirBeamLink,
  private val engine: RecordingEngine,
) : ViewModel() {
  val recording: StateFlow<Boolean> =
    engine.recording.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

  private val _startState = MutableStateFlow<StartState>(StartState.Idle)
  val startState: StateFlow<StartState> = _startState.asStateFlow()

  fun start(form: NewSessionForm) {
    if (_startState.value == StartState.Starting) return
    _startState.value = StartState.Starting
    viewModelScope.launch {
      val target = (link.state.value as? LinkState.Connected)?.target
      val result = when {
        form.method != RecordingMethod.AirBeamMobile -> ConfigResult.BadConfigFailure
        target == null -> ConfigResult.NotConnected
        else -> engine.start(target, NewSessionDetails(form.name, form.tags))
      }
      _startState.value = if (result == ConfigResult.Success) StartState.Started else StartState.Failed(result)
    }
  }

  fun stop() {
    viewModelScope.launch { engine.stop() }
  }
}