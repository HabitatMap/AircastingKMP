package pl.llp.aircasting.data.auth

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthState {
  data object SignedOut : AuthState
  data class SignedIn(val token: String) : AuthState
}

val AuthState.tokenOrNull: String? get() = (this as? AuthState.SignedIn)?.token

interface AuthSession {
  val state: StateFlow<AuthState>
  fun start(token: String)
  fun end()
}

class StoredAuthSession(private val settings: Settings) : AuthSession {

  private val _state = MutableStateFlow(settings.readState())
  override val state: StateFlow<AuthState> = _state.asStateFlow()

  override fun start(token: String) {
    settings.putString(KeyToken, token)
    _state.value = AuthState.SignedIn(token)
  }

  override fun end() {
    settings.remove(KeyToken)
    _state.value = AuthState.SignedOut
  }
}

private const val KeyToken = "auth_token"

private fun Settings.readState(): AuthState =
  getStringOrNull(KeyToken)?.let(AuthState::SignedIn) ?: AuthState.SignedOut