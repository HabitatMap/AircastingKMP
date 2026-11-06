package pl.llp.aircasting.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.LinkState
import pl.llp.aircasting.recording.FixedSessionStarter
import pl.llp.aircasting.recording.FixedStartResult
import pl.llp.aircasting.recording.NewSessionDetails
import pl.llp.aircasting.recording.PhoneLocation
import pl.llp.aircasting.recording.RecordingEngine
import pl.llp.aircasting.recording.Uplink
import kotlin.time.Duration.Companion.seconds

/** How long Start waits for a fix before recording without one (e.g. indoors). */
internal val LocatingTimeout = 10.seconds

sealed interface StartState {
  data object Idle : StartState

  /** Start was tapped; [step] is what it is waiting on before the session records. */
  data class Preparing(val step: PrepareStep) : StartState
  data object Started : StartState
  data class Failed(val result: ConfigResult) : StartState

  /** Fixed only: the backend must create the session, and that needs an account. */
  data object SignedOut : StartState

  /** Fixed only: the backend refused; the AirBeam was not touched. */
  data object ServerError : StartState
}

enum class PrepareStep { Locating, Configuring }

/** The UI's handle on [RecordingEngine]: the wizard starts a session, the shell stops it. */
class RecordingViewModel(
  private val link: AirBeamLink,
  private val engine: RecordingEngine,
  private val location: PhoneLocation,
  private val fixedStarter: FixedSessionStarter,
) : ViewModel() {
  private val log = Logger.withTag("RecordingViewModel")

  val recording: StateFlow<Boolean> =
    engine.recording.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

  private val _startState = MutableStateFlow<StartState>(StartState.Idle)
  val startState: StateFlow<StartState> = _startState.asStateFlow()

  fun start(form: NewSessionForm) {
    if (_startState.value is StartState.Preparing) return
    val fixed = form.method == RecordingMethod.AirBeamFixed
    // A fixed session's place comes from the form, not the phone: no fix to wait for.
    _startState.value =
      StartState.Preparing(if (fixed) PrepareStep.Configuring else PrepareStep.Locating)
    viewModelScope.launch {
      _startState.value = if (fixed) startFixed(form) else startMobile(form)
    }
  }

  private suspend fun startMobile(form: NewSessionForm): StartState {
    val picked = form.device
    val result = when {
      form.method != RecordingMethod.AirBeamMobile -> ConfigResult.BadConfigFailure
      else -> {
        locate()
        _startState.value = StartState.Preparing(PrepareStep.Configuring)
        if (picked == null || !linkTo(picked)) ConfigResult.NotConnected
        else engine.start(picked, NewSessionDetails(form.name, form.tags))
      }
    }
    return if (result == ConfigResult.Success) StartState.Started else StartState.Failed(result)
  }

  /** Mini V2 waits up to 30 s for its first upload, so this stays on Configuring that long. */
  private suspend fun startFixed(form: NewSessionForm): StartState {
    val picked = form.device
    val placement = form.placement
    if (picked == null || placement == null) return StartState.Failed(ConfigResult.BadConfigFailure)
    if (!linkTo(picked)) return StartState.Failed(ConfigResult.NotConnected)
    val details = NewSessionDetails(form.name, form.tags)
    return when (val result = fixedStarter.start(picked, details, placement, Uplink.Wifi(form.wifi))) {
      FixedStartResult.Started -> StartState.Started
      FixedStartResult.SignedOut -> StartState.SignedOut
      FixedStartResult.ServerError -> StartState.ServerError
      is FixedStartResult.DeviceFailed -> StartState.Failed(result.result)
    }
  }

  /**
   * Waits for a real fix before the device is configured: readings start right after, and any
   * that arrive before the phone's first fix are stamped with the default location (New York).
   */
  private suspend fun locate() {
    if (withTimeoutOrNull(LocatingTimeout) { location.awaitFix() } == null) {
      log.w { "No fix within $LocatingTimeout; recording from the last known location" }
    }
  }

  /** True once the link is on [device]: reuses a link already there, else connects (replacing any other). */
  private suspend fun linkTo(device: DiscoveredAirBeam): Boolean {
    val current = link.state.value
    if (current is LinkState.Connected && current.target.id == device.id) return true
    return link.connect(device) is LinkState.Connected
  }

  fun stop() {
    viewModelScope.launch { engine.stop() }
  }

  fun clearFailure() {
    if (_startState.value !is StartState.Preparing) _startState.value = StartState.Idle
  }
}