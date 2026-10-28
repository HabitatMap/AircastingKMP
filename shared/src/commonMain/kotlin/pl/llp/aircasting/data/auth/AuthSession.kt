package pl.llp.aircasting.data.auth

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthState {
  data object SignedOut : AuthState
  /** [profile] is null only for a token stored by a build that didn't keep profiles. */
  data class SignedIn(val token: String, val profile: AccountProfile?) : AuthState
}

val AuthState.tokenOrNull: String? get() = (this as? AuthState.SignedIn)?.token

interface AuthSession {
  val state: StateFlow<AuthState>
  fun start(token: String, profile: AccountProfile)
  /**
   * [token] is the one the profile was fetched with. Ignored unless it's still the session's:
   * a late response must not revive an ended session or land in someone else's.
   */
  fun updateProfile(token: String, profile: AccountProfile)
  fun end()
}

class StoredAuthSession(private val settings: Settings) : AuthSession {

  private val _state = MutableStateFlow(settings.readState())
  override val state: StateFlow<AuthState> = _state.asStateFlow()

  override fun start(token: String, profile: AccountProfile) {
    settings.putString(KeyToken, token)
    settings.putProfile(profile)
    _state.value = AuthState.SignedIn(token, profile)
  }

  override fun updateProfile(token: String, profile: AccountProfile) {
    val signedIn = (_state.value as? AuthState.SignedIn)?.takeIf { it.token == token } ?: return
    settings.putProfile(profile)
    _state.value = signedIn.copy(profile = profile)
  }

  override fun end() {
    settings.remove(KeyToken)
    settings.remove(KeyName)
    settings.remove(KeyEmail)
    _state.value = AuthState.SignedOut
  }
}

// Same key names as the legacy app's Settings, so migrating its prefs is a straight copy.
private const val KeyToken = "auth_token"
private const val KeyName = "profile_name"
private const val KeyEmail = "email"

private fun Settings.readState(): AuthState {
  val token = getStringOrNull(KeyToken) ?: return AuthState.SignedOut
  return AuthState.SignedIn(token, readProfile())
}

private fun Settings.readProfile(): AccountProfile? {
  val name = getStringOrNull(KeyName) ?: return null
  val email = getStringOrNull(KeyEmail) ?: return null
  return AccountProfile(name, email)
}

private fun Settings.putProfile(profile: AccountProfile) {
  putString(KeyName, profile.name)
  putString(KeyEmail, profile.email)
}

val AuthState.isSignedIn: Boolean get() = this is AuthState.SignedIn
