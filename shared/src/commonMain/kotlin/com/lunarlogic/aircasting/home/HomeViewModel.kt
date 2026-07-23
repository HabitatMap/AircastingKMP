package com.lunarlogic.aircasting.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
  private val repository: HomeRepository,
  private val location: LocationProvider,
) : ViewModel() {

  private val _state = MutableStateFlow<HomeScreenState>(HomeScreenState.Loading)
  val state: StateFlow<HomeScreenState> = _state.asStateFlow()

  private var loadJob: Job? = null

  fun refresh() {
    // Resumes can arrive back-to-back; a new load cancels the in-flight one (last wins).
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      // Spinner only when there's nothing to show yet. A reload keeps the current
      // content on screen, so returning to the app never flashes Loading.
      if (_state.value !is HomeScreenState.Content) _state.value = HomeScreenState.Loading
      val loc = location.current()
      _state.value = runCatching { repository.load(loc) }
        .fold(
          onSuccess = { HomeScreenState.Content(it) },
          onFailure = { HomeScreenState.Error },
        )
    }
  }
}