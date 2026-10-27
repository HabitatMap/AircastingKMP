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

  fun startPasswordChange() = updateContent { it.copy(password = PasswordChange.Open()) }

  fun cancelPasswordChange() = updateContent { it.copy(password = PasswordChange.None) }

  fun changePassword(current: String, new: String, confirmation: String) = viewModelScope.launch {
    // Checked here, not left to the backend: it's free locally and a 422 round trip isn't.
    if (new != confirmation) {
      return@launch showPasswordErrors(mapOf(PasswordField.Confirmation to EditError.Rejected(PasswordMismatch)))
    }
    runCatching { repository.changePassword(current, new) }
      .onSuccess { cancelPasswordChange() }
      .onFailure {
        log.e(it) { "password change failed" }
        showPasswordErrors(it.toPasswordErrors())
      }
  }

  fun startAccountDeletion() = updateDeletion { Deletion.Confirming }

  fun cancelAccountDeletion() = updateDeletion { Deletion.None }

  fun sendDeletionCode() = viewModelScope.launch {
    runCatching { repository.requestAccountDeletion() }
      .onSuccess { updateDeletion { Deletion.AwaitingCode() } }
      // Stay on the confirmation: no code was mailed, so asking for one would strand the user.
      .onFailure { log.e(it) { "deletion code request failed" } }
  }

  fun submitDeletionCode(code: String) = viewModelScope.launch {
    runCatching { repository.confirmAccountDeletion(code) }
      .onSuccess { _sessionEnded.emit(Unit) }
      .onFailure {
        log.e(it) { "deletion confirmation rejected" }
        updateDeletion { Deletion.AwaitingCode(isRejected = true) }
      }
  }

  fun startEdit(field: ProfileField) = updateContent { it.copy(edit = ProfileEdit.Open(field)) }

  fun cancelEdit() = updateContent { it.copy(edit = ProfileEdit.None) }

  fun saveEdit(input: String) = viewModelScope.launch {
    val content = _state.value as? AccountScreenState.Content ?: return@launch
    val edit = content.edit as? ProfileEdit.Open ?: return@launch
    val value = input.trim()
    // The backend 422s an update with nothing to change, so don't send one.
    if (value == content.profile.valueOf(edit.field)) return@launch cancelEdit()
    runCatching {
      when (edit.field) {
        ProfileField.Email -> repository.updateProfile(email = value)
        ProfileField.Username -> repository.updateProfile(name = value)
      }
    }
      .onSuccess { _state.value = AccountScreenState.Content(it) }
      .onFailure {
        log.e(it) { "profile update failed" }
        updateContent { c -> c.copy(edit = edit.copy(error = it.toEditError(edit.field))) }
      }
  }

  private fun showPasswordErrors(errors: Map<PasswordField, EditError>) =
    updateContent { it.copy(password = PasswordChange.Open(errors)) }

  private fun updateDeletion(next: (Deletion) -> Deletion) =
    updateContent { it.copy(deletion = next(it.deletion)) }

  private fun updateContent(transform: (AccountScreenState.Content) -> AccountScreenState.Content) {
    _state.update { s -> if (s is AccountScreenState.Content) transform(s) else s }
  }
}

private fun Throwable.toEditError(field: ProfileField): EditError =
  if (this is AccountUpdateRejected)
  // No entry for this field (e.g. "nothing to change") → Refused: the top-level message is
  // English-only prose, so the UI shows its own translated text instead.
    fields[field.wireName]?.firstOrNull()?.let(EditError::Rejected) ?: EditError.Refused
  else
    EditError.Unexpected

private fun Throwable.toPasswordErrors(): Map<PasswordField, EditError> {
  // Anything not tied to a field goes on the last one, the field right above Save.
  if (this !is AccountUpdateRejected) return mapOf(PasswordField.Confirmation to EditError.Unexpected)
  val byField = PasswordField.entries
    .mapNotNull { field -> fields[field.wireName]?.firstOrNull()?.let { field to EditError.Rejected(it) } }
    .toMap()
  return byField.ifEmpty { mapOf(PasswordField.Confirmation to EditError.Refused) }
}

private val ProfileField.wireName
  get() = when (this) {
    ProfileField.Email -> "email"
    ProfileField.Username -> "username"
  }