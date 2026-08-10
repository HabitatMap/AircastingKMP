package pl.llp.aircasting.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.llp.aircasting.data.auth.AuthState

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
  private val log = Logger.withTag("Auth")

  private val _form = MutableStateFlow(AuthFormState())
  val form: StateFlow<AuthFormState> = _form.asStateFlow()

  val session: StateFlow<AuthState> = repository.state

  fun switchTo(mode: AuthMode) = _form.update { it.copy(mode = mode, failure = null) }
  fun onLoginChange(value: String) = edit { it.copy(login = value) }
  fun onEmailChange(value: String) = edit { it.copy(email = value) }
  fun onUsernameChange(value: String) = edit { it.copy(username = value) }
  fun onPasswordChange(value: String) = edit { it.copy(password = value) }

  fun togglePasswordVisibility() = _form.update { it.copy(passwordVisible = !it.passwordVisible) }

  fun submit() {
    val form = _form.value
    if (!form.canSubmit) return          // the double-tap guard; `submitting` feeds canSubmit
    _form.value = form.copy(submitting = true, failure = null)
    viewModelScope.launch {
      val result = when (form.mode) {
        AuthMode.SignIn -> repository.signIn(form.login.trim(), form.password)
        AuthMode.SignUp ->
          repository.signUp(form.email.trim(), form.username.trim(), form.password)
      }
      result.onFailure { log.w(it) { "${form.mode} failed" } }
      _form.update { it.copy(submitting = false, failure = result.exceptionOrNull()?.asFailure()) }
    }
  }

  private fun edit(change: (AuthFormState) -> AuthFormState) =
    _form.update { change(it).copy(failure = null) }
}

private fun Throwable.asFailure(): AuthFailure = this as? AuthFailure ?: AuthFailure.Unexpected