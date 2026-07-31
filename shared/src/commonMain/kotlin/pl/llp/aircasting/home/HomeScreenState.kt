package pl.llp.aircasting.home

sealed interface HomeScreenState {
  data object Loading : HomeScreenState
  data class Content(val ui: HomeUiState) : HomeScreenState
  data object Error : HomeScreenState
}