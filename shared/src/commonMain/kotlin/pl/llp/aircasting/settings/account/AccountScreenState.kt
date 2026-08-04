package pl.llp.aircasting.settings.account

sealed interface AccountScreenState {
  data object Loading : AccountScreenState
  data class Content(
    val profile: AccountProfile,
    val awaitingDeletionCode: Boolean = false,
  ) : AccountScreenState
  data object Error : AccountScreenState
}
