package pl.llp.aircasting.settings.account

sealed interface AccountScreenState {
  data object Loading : AccountScreenState
  data class Content(
    val profile: AccountProfile,
    val deletion: Deletion = Deletion.None,
  ) : AccountScreenState
  data object Error : AccountScreenState
}

sealed interface Deletion {
  data object None : Deletion
  data object Confirming : Deletion
  data class AwaitingCode(val rejected: Boolean = false) : Deletion
}