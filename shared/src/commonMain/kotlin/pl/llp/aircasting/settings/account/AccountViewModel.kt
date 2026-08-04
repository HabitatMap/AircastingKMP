package pl.llp.aircasting.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {

  private val log = Logger.withTag("Account")

  private val _state = MutableStateFlow<AccountScreenState>(AccountScreenState.Loading)
  val state: StateFlow<AccountScreenState> = _state.asStateFlow()
  private val _sessionEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val sessionEnded: SharedFlow<Unit> = _sessionEnded.asSharedFlow()

  fun refresh() = viewModelScope.launch {
    _state.value = runCatching { repository.profile() }.fold(
      onSuccess = { AccountScreenState.Content(it) },
      onFailure = {
        // Covers both a 401 (token expired) and the missing-token invariant violation. Once a
        // login screen exists, a 401 should end the session instead of showing Error.
        log.e(it) { "profile load failed" }
        AccountScreenState.Error
      },
    )
  }
  fun signOut() = viewModelScope.launch {
    repository.signOut()
    _sessionEnded.emit(Unit)
  }

  fun resetPassword() = viewModelScope.launch {
    // Only the loaded profile's address may be submitted — the endpoint takes a login, and any
    // other value would mail a reset link to someone else's account.
    val email = (_state.value as? AccountScreenState.Content)?.profile?.email ?: return@launch
    runCatching { repository.requestPasswordReset(email) }
      .onFailure { log.e(it) { "password reset failed" } }
  }
  fun requestAccountDeletion() = viewModelScope.launch {
    runCatching { repository.requestAccountDeletion() }
      .onSuccess {
        _state.update { s -> if (s is AccountScreenState.Content) s.copy(awaitingDeletionCode = true) else s }
      }
      .onFailure { log.e(it) { "deletion code request failed" } }
  }

  fun confirmAccountDeletion(code: String) = viewModelScope.launch {
    runCatching { repository.confirmAccountDeletion(code) }
      .onSuccess { _sessionEnded.emit(Unit) }
      // 401 = wrong or expired (30 min) code. Stay put with the prompt open so it can be retyped.
      .onFailure { log.e(it) { "deletion confirmation rejected" } }
  }
}