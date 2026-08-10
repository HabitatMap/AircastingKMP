package pl.llp.aircasting.auth

import kotlinx.coroutines.flow.StateFlow
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.AuthState
import pl.llp.aircasting.data.network.AuthApi
import pl.llp.aircasting.data.network.AuthResponse
import kotlin.coroutines.cancellation.CancellationException

interface AuthRepository {
  val state: StateFlow<AuthState>
  suspend fun signIn(login: String, password: String): Result<Unit>
  suspend fun signUp(email: String, username: String, password: String): Result<Unit>
  fun signOut()
}

class NetworkAuthRepository(
  private val api: AuthApi,
  private val session: AuthSession,
) : AuthRepository {

  override val state: StateFlow<AuthState> = session.state

  override suspend fun signIn(login: String, password: String): Result<Unit> =
    authenticate { api.signIn(login, password) }

  override suspend fun signUp(email: String, username: String, password: String): Result<Unit> =
    authenticate { api.signUp(email, username, password) }

  override fun signOut() = session.end()

  private suspend fun authenticate(call: suspend () -> AuthResponse): Result<Unit> = try {
    when (val response = call()) {
      is AuthResponse.Authenticated -> {
        // Starting the session is the *only* success signal. Everything watching `state`
        // — the gate, the sheet, Account — reacts off this one write.
        session.start(response.user.authenticationToken)
        Result.success(Unit)
      }
      AuthResponse.Rejected -> Result.failure(AuthFailure.InvalidCredentials)
      is AuthResponse.Invalid -> Result.failure(
        AuthFailure.Invalid(
          email = response.errors.email.firstOrNull(),
          username = response.errors.username.firstOrNull(),
          password = response.errors.password.firstOrNull(),
        ),
      )
      AuthResponse.Failed -> Result.failure(AuthFailure.Unexpected)
    }
  } catch (cancelled: CancellationException) {
    // Never swallow this: cancellation is coroutine control flow, not an auth error. If the
    // user navigates away mid-request, turning that into "Unexpected" would flash a bogus
    // error on a screen that is already gone. `runCatching` gets this wrong, which is why
    // this is a hand-written try/catch.
    throw cancelled
  } catch (offline: Exception) {
    Result.failure(AuthFailure.Unexpected)
  }
}
