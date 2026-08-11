package pl.llp.aircasting.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.llp.aircasting.settings.account.AccountRepository
import kotlin.coroutines.cancellation.CancellationException

class ForgotPasswordViewModel(private val account: AccountRepository) : ViewModel() {

  private val log = Logger.withTag("Auth")

  private val _state = MutableStateFlow(ForgotPasswordState())
  val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

  fun onEmailChange(value: String) =
    _state.update { it.copy(email = value, status = ForgotPasswordState.Status.Editing) }

  fun submit() {
    val current = _state.value
    if (!current.canSubmit) return
    _state.value = current.copy(status = ForgotPasswordState.Status.Sending)
    viewModelScope.launch {
      val sent = try {
        account.requestPasswordReset(current.email.trim())
        true
      } catch (cancelled: CancellationException) {
        throw cancelled                       // control flow, not a failure — see AuthRepository
      } catch (failure: Exception) {
        log.w(failure) { "password reset failed" }
        false
      }
      _state.update {
        it.copy(
          status =
            if (sent) ForgotPasswordState.Status.Sent else ForgotPasswordState.Status.Failed,
        )
      }
    }
  }
}