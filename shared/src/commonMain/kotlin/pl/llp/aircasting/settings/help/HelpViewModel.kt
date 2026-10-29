package pl.llp.aircasting.settings.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HelpScreenState {
  data object Loading : HelpScreenState
  data class Content(val links: HelpLinks) : HelpScreenState
  data object Error : HelpScreenState
}

class HelpViewModel(private val repository: HelpRepository) : ViewModel() {

  private val log = Logger.withTag("Help")

  private val _state = MutableStateFlow<HelpScreenState>(HelpScreenState.Loading)
  val state: StateFlow<HelpScreenState> = _state.asStateFlow()

  fun load(locale: String) = viewModelScope.launch {
    _state.value = HelpScreenState.Loading
    _state.value = runCatching { repository.links(locale) }.fold(
      onSuccess = HelpScreenState::Content,
      onFailure = {
        log.e(it) { "app config load failed" }
        HelpScreenState.Error
      },
    )
  }
}