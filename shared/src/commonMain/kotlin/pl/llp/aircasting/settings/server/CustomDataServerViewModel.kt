package pl.llp.aircasting.settings.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.network.ServerProbe
import pl.llp.aircasting.settings.app.AppSettingsRepository
import kotlin.time.Duration.Companion.seconds

val TooLongThreshold = 10.seconds

sealed interface CustomServerStep {
  data object Intro : CustomServerStep
  data class Form(val form: CustomServerForm = CustomServerForm()) : CustomServerStep
  data class Testing(val baseUrl: String, val tookTooLong: Boolean = false) : CustomServerStep
  data class Verified(val baseUrl: String) : CustomServerStep
  data class Failed(val baseUrl: String) : CustomServerStep
}

val CustomServerStep.progress: Float
  get() = when (this) {
    CustomServerStep.Intro -> 1f / 3
    is CustomServerStep.Form -> 2f / 3
    else -> 1f
  }

class CustomDataServerViewModel(
  private val settings: AppSettingsRepository,
  private val probe: ServerProbe,
  private val session: AuthSession,
) : ViewModel() {
  private val _step = MutableStateFlow<CustomServerStep>(CustomServerStep.Intro)
  val step: StateFlow<CustomServerStep> = _step.asStateFlow()

  private val _finished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val finished: SharedFlow<Unit> = _finished.asSharedFlow()

  private var probeJob: Job? = null

  fun edit(form: CustomServerForm) {
    _step.value = CustomServerStep.Form(form)
  }

  fun next() {
    when (val step = _step.value) {
      CustomServerStep.Intro ->
        _step.value = CustomServerStep.Form(
          CustomServerForm.from(settings.preferences.value.dataServerUrl),
        )
      is CustomServerStep.Form -> step.form.baseUrl?.let(::test)
      else -> Unit
    }
  }

  fun back() {
    when (val step = _step.value) {
      is CustomServerStep.Form -> _step.value = CustomServerStep.Intro
      is CustomServerStep.Testing -> cancelTest()
      is CustomServerStep.Failed -> _step.value =
        CustomServerStep.Form(CustomServerForm.from(step.baseUrl))
      is CustomServerStep.Verified -> _step.value =
        CustomServerStep.Form(CustomServerForm.from(step.baseUrl))
      else -> Unit
    }
  }

  fun cancelTest() {
    val testing = _step.value as? CustomServerStep.Testing ?: return
    probeJob?.cancel()
    _step.value = CustomServerStep.Form(CustomServerForm.from(testing.baseUrl))
  }

  fun save() = viewModelScope.launch {
    val verified = _step.value as? CustomServerStep.Verified ?: return@launch
    applyServer(verified.baseUrl)
  }

  fun useOfficialServer() = viewModelScope.launch { applyServer(null) }

  private suspend fun applyServer(baseUrl: String?) {
    settings.update { it.copy(dataServerUrl = baseUrl) }
    // A token is only valid against the server that issued it.
    session.end()
    _finished.emit(Unit)
  }

  private fun test(baseUrl: String) {
    _step.value = CustomServerStep.Testing(baseUrl)
    probeJob = viewModelScope.launch {
      val nag = launch {
        delay(TooLongThreshold)
        (_step.value as? CustomServerStep.Testing)?.let { _step.value = it.copy(tookTooLong = true) }
      }
      val reachable = probe.reachable(baseUrl)
      nag.cancel()
      _step.value =
        if (reachable) CustomServerStep.Verified(baseUrl) else CustomServerStep.Failed(baseUrl)
    }
  }
}