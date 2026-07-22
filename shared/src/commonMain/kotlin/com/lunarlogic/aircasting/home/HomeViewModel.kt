package com.lunarlogic.aircasting.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

  init { refresh() }

  fun refresh() {
    viewModelScope.launch {
      _state.value = HomeScreenState.Loading
      _state.value = runCatching { repository.load(location.current()) }
        .fold(
          onSuccess = { HomeScreenState.Content(it) },
          onFailure = { HomeScreenState.Error },
        )
    }
  }
}