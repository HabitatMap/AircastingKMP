package pl.llp.aircasting.record

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NewSessionState(
  val step: NewSessionStep = NewSessionStep.Method,
  val form: NewSessionForm = NewSessionForm(),
  val recentTags: List<String> = emptyList(),
) {
  val canContinue: Boolean
    get() = when (step) {
      NewSessionStep.Method -> form.method != null
      NewSessionStep.Details -> form.name.isNotBlank()
      NewSessionStep.Confirm -> true
    }
}

class NewSessionViewModel : ViewModel() {
  private val _state = MutableStateFlow(NewSessionState())
  val state: StateFlow<NewSessionState> = _state.asStateFlow()

  fun select(method: RecordingMethod) = edit { it.copy(method = method) }

  fun rename(name: String) = edit { it.copy(name = name) }

  fun editTags(input: String) = edit { it.copy(tagsInput = input) }

  fun addTag(tag: String) = edit { form ->
    val clean = tag.trim()
    if (clean.isEmpty() || clean in form.tags) form
    else form.copy(tagsInput = (form.tags + clean).joinToString(", "))
  }

  fun setInterval(interval: SamplingInterval) = edit { it.copy(interval = interval) }

  fun next() = _state.update { state ->
    if (!state.canContinue) state
    else state.copy(step = NewSessionStep.entries.getOrElse(state.step.ordinal + 1) { state.step })
  }

  fun back() = _state.update { state ->
    state.copy(step = NewSessionStep.entries.getOrElse(state.step.ordinal - 1) { state.step })
  }

  private fun edit(block: (NewSessionForm) -> NewSessionForm) =
    _state.update { it.copy(form = block(it.form)) }
}