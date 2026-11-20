package pl.llp.aircasting.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.llp.aircasting.bluetooth.BluetoothRadio
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.recording.PhoneLocation

data class NewSessionState(
  val step: NewSessionStep = NewSessionStep.Method,
  val form: NewSessionForm = NewSessionForm(),
  val recentTags: List<String> = emptyList(),
  /** Watched only once an AirBeam method is picked; decides whether the Bluetooth step is walked. */
  val bluetoothOff: Boolean = false,
) {
  val canContinue: Boolean
    get() = when (step) {
      NewSessionStep.Method -> form.method != null
      NewSessionStep.Device -> form.device != null
      NewSessionStep.Location -> form.placement != null
      NewSessionStep.Bluetooth -> !bluetoothOff
      NewSessionStep.PowerOn, NewSessionStep.Confirm -> true
      NewSessionStep.Details -> form.name.isNotBlank() &&
        (form.method != RecordingMethod.AirBeamFixed || form.cellular || form.hasWifi)

    }
  /** Recent tags that complete the word being typed. */
  val matchingTags: List<String>
    get() = if (form.tagDraft.isEmpty()) emptyList()
    else recentTags.filter { it.startsWith(form.tagDraft, ignoreCase = true) && it !in form.tags }
}

private val TagSeparators = Regex("[\\s,]+")

private fun List<String>.plusTags(words: List<String>) =
  (this + words.map { it.trim() }.filter { it.isNotEmpty() }).distinct()

class NewSessionViewModel(
  private val location: PhoneLocation,
  private val radio: BluetoothRadio,
) : ViewModel() {
  private val _state = MutableStateFlow(NewSessionState())
  val state: StateFlow<NewSessionState> = _state.asStateFlow()

  init {
    // Warm the GPS while the user fills the wizard, as legacy `startNewSessionWizard` did,
    // so Start usually finds a fix ready. Stops with the wizard.
    viewModelScope.launch { location.track() }
    // Known before Next is tapped, so the wizard routes to the Bluetooth step instead of showing
    // the next screen and swapping it out. Not watched for the phone mic: on iOS the first
    // CBCentralManager is what raises the Bluetooth permission prompt.
    viewModelScope.launch {
      _state.map { it.form.usesAirBeam }.distinctUntilChanged().collectLatest { airBeam ->
        if (airBeam) radio.isOff.collect(::onBluetoothOff)
      }
    }
  }

  /**
   * Turned on while on the Bluetooth step: carry on to power-on. Turned off before a device is
   * picked: back to the Bluetooth step, since the scan can't run without it.
   */
  private fun onBluetoothOff(off: Boolean) = _state.update { state ->
    when {
      !off && state.step == NewSessionStep.Bluetooth -> state.copy(bluetoothOff = false, step = NewSessionStep.PowerOn)
      off && state.step in NeedsBluetooth -> state.copy(bluetoothOff = true, step = NewSessionStep.Bluetooth)
      else -> state.copy(bluetoothOff = off)
    }
  }

  fun select(method: RecordingMethod) = edit { it.copy(method = method) }

  fun rename(name: String) = edit { it.copy(name = name) }

  /**
   * A space or comma ends a tag: tag_list is space-separated, so a tag can't hold one
   * (DOMAIN_KNOWLEDGE.md "Tags"). Everything before the last separator becomes chips.
   */
  fun editTagDraft(text: String) = edit { form ->
    val words = text.split(TagSeparators)
    form.copy(committedTags = form.committedTags.plusTags(words.dropLast(1)), tagDraft = words.last())
  }
  /** Outdoor drops the pin where the phone is, or New York without a fix (legacy ChooseLocationViewMvcImpl). */
  fun setIndoor(indoor: Boolean) = edit {
    it.copy(indoor = indoor, outdoorLocation = it.outdoorLocation ?: if (indoor) null else location.latest() ?: NewYork)
  }

  fun commitTagDraft() = edit { it.copy(committedTags = it.committedTags.plusTags(listOf(it.tagDraft)), tagDraft = "") }

  /** A suggestion replaces the half-typed word it completes. */
  fun addTag(tag: String) = edit { it.copy(committedTags = it.committedTags.plusTags(listOf(tag)), tagDraft = "") }

  fun removeTag(tag: String) = edit { it.copy(committedTags = it.committedTags - tag) }

  fun setInterval(interval: SamplingInterval) = edit { it.copy(interval = interval) }

  fun placePin(location: GeoLocation) = edit { it.copy(outdoorLocation = location) }

  fun editWifiName(ssid: String) = edit { it.copy(wifi = it.wifi.copy(ssid = ssid)) }

  fun editWifiPassword(password: String) = edit { it.copy(wifi = it.wifi.copy(password = password)) }

  fun next() = _state.update { state -> if (!state.canContinue) state else state.moveBy(1) }

  fun back() = _state.update { it.moveBy(-1) }

  fun setCellular(cellular: Boolean) = edit { it.copy(cellular = cellular && it.offersCellular) }

  fun selectDevice(device: DiscoveredAirBeam) = edit {
    it.copy(device = device).run { copy(cellular = cellular && offersCellular) }
  }

  private fun NewSessionState.moveBy(offset: Int): NewSessionState {
    val steps = form.steps(bluetoothOff)
    return copy(step = steps.getOrElse(steps.indexOf(step) + offset) { step })
  }

  private fun edit(block: (NewSessionForm) -> NewSessionForm) =
    _state.update { it.copy(form = block(it.form)) }
}

/** Both are required (legacy); a password of spaces is still a password. */
private val NewSessionForm.hasWifi: Boolean
  get() = wifi.ssid.isNotBlank() && wifi.password.isNotEmpty()

private val NewYork = GeoLocation(40.7128, -74.0060)

private val NeedsBluetooth = setOf(NewSessionStep.PowerOn, NewSessionStep.Device)