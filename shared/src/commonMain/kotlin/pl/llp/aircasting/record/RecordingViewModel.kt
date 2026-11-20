package pl.llp.aircasting.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.LinkState
import pl.llp.aircasting.recording.FixedSessionStarter
import pl.llp.aircasting.recording.FixedStartResult
import pl.llp.aircasting.recording.MicrophoneRecorder
import pl.llp.aircasting.recording.NewSessionDetails
import pl.llp.aircasting.recording.PendingFixedSession
import pl.llp.aircasting.recording.PhoneLocation
import pl.llp.aircasting.recording.RecordingEngine
import kotlin.time.Duration.Companion.seconds

/** How long Start waits for a fix before recording without one (e.g. indoors). */
internal val LocatingTimeout = 10.seconds

sealed interface StartState {
  data object Idle : StartState

  /** Start was tapped; [step] is what it is waiting on before the session records. */
  data class Preparing(val step: PrepareStep) : StartState
  data object Started : StartState
  data class Failed(val result: ConfigResult) : StartState

  /** Mic only: one mic session at a time (legacy). */
  data object MicBusy : StartState

  /** Fixed only: the backend must create the session, and that needs an account. */
  data object SignedOut : StartState

  /** Fixed only: the backend refused; the AirBeam was not touched. */
  data object ServerError : StartState
}

enum class PrepareStep { Locating, Configuring }

/** The wizard's Connect step: the outcome of the last [RecordingViewModel.connect]. */
enum class ConnectState { Idle, Connecting, Connected, Failed }

/** The UI's handle on [RecordingEngine]: the wizard starts a session, the shell stops it. */
class RecordingViewModel(
  private val link: AirBeamLink,
  private val engine: RecordingEngine,
  private val location: PhoneLocation,
  private val fixedStarter: FixedSessionStarter,
  private val mic: MicrophoneRecorder,
  private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ViewModel() {

  private var openedLink = false

  private val _connectState = MutableStateFlow(ConnectState.Idle)
  val connectState: StateFlow<ConnectState> = _connectState.asStateFlow()
  private var connectAttempt = 0

  fun connect(device: DiscoveredAirBeam) {
    openedLink = true
    // Set before launching, so the Connect step never renders a stale outcome first.
    _connectState.value = ConnectState.Connecting
    val attempt = ++connectAttempt
    viewModelScope.launch {
      val connected = linkTo(device)
      // A retry may have started meanwhile; only the latest attempt speaks for the screen.
      if (attempt == connectAttempt) {
        _connectState.value = if (connected) ConnectState.Connected else ConnectState.Failed
      }
    }
  }

  /** Joins an attempt already on its way to [device]; reuses a link already on it; else connects. */
  private suspend fun linkTo(device: DiscoveredAirBeam): Boolean {
    val current = link.state.first { !(it is LinkState.Connecting && it.target.id == device.id) }
    if (current is LinkState.Connected && current.target.id == device.id) return true
    return link.connect(device) is LinkState.Connected
  }

  override fun onCleared() {
    if (openedLink) appScope.launch { link.disconnect() }
  }

  private val log = Logger.withTag("RecordingViewModel")

  val recording: StateFlow<Boolean> =
    engine.recording.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

  private val _startState = MutableStateFlow<StartState>(StartState.Idle)
  val startState: StateFlow<StartState> = _startState.asStateFlow()

  /**
   * Fixed only: the backend holds this session but the AirBeam refused it. Start again reuses it
   * instead of creating a second session, so the wizard must not change what was registered.
   */
  private val pending = MutableStateFlow<PendingFixedSession?>(null)
  val sessionRegistered: StateFlow<Boolean> =
    pending.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

  fun start(form: NewSessionForm) {
    if (_startState.value is StartState.Preparing) return
    val fixed = form.method == RecordingMethod.AirBeamFixed
    // A fixed session's place comes from the form, not the phone: no fix to wait for.
    _startState.value =
      StartState.Preparing(if (fixed) PrepareStep.Configuring else PrepareStep.Locating)
    viewModelScope.launch {
      _startState.value = when (form.method) {
        RecordingMethod.AirBeamFixed -> startFixed(form)
        RecordingMethod.PhoneMicrophone -> startMic(form)
        else -> startMobile(form)
      }
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
        else engine.start(picked, NewSessionDetails(form.name, form.tags), form.interval.every)
      }
    }
    return if (result == ConfigResult.Success) {
      openedLink = false // the session records over this link now; leaving must not cut it
      StartState.Started
    } else StartState.Failed(result)
  }

  /** Like AirBeam mobile, readings are stamped with the phone's fix, so wait for one first. */
  private suspend fun startMic(form: NewSessionForm): StartState {
    locate()
    return if (mic.start(
        NewSessionDetails(
          form.name,
          form.tags
        )
      )
    ) StartState.Started else StartState.MicBusy
  }

  /** Mini V2 waits up to 30 s for its first upload, so this stays on Configuring that long. */
  private suspend fun startFixed(form: NewSessionForm): StartState {
    val picked = form.device
    val placement = form.placement
    if (picked == null || placement == null) return StartState.Failed(ConfigResult.BadConfigFailure)
    if (!linkTo(picked)) return StartState.Failed(ConfigResult.NotConnected)
    val retry = pending.value
    val result = if (retry != null) fixedStarter.configure(retry, form.uplink)
    else fixedStarter.start(picked, NewSessionDetails(form.name, form.tags), placement, form.uplink)
    return when (result) {
      FixedStartResult.Started -> StartState.Started
      FixedStartResult.SignedOut -> StartState.SignedOut
      FixedStartResult.ServerError -> StartState.ServerError
      is FixedStartResult.DeviceFailed -> {
        result.pending?.let { pending.value = it }
        StartState.Failed(result.result)
      }
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

  fun stop() {
    viewModelScope.launch { engine.stop() }
  }

  fun clearFailure() {
    if (_startState.value !is StartState.Preparing) _startState.value = StartState.Idle
  }
}