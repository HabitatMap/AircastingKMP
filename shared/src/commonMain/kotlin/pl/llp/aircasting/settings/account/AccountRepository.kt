package pl.llp.aircasting.settings.account

import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.tokenOrNull
import pl.llp.aircasting.data.network.AccountApi
import pl.llp.aircasting.data.network.UserDto

interface AccountRepository {
  suspend fun profile(): AccountProfile
  suspend fun signOut()
  suspend fun requestPasswordReset(login: String)
  suspend fun requestAccountDeletion()
  suspend fun confirmAccountDeletion(code: String)
}

class NetworkAccountRepository(
  private val api: AccountApi,
  private val session: AuthSession,
) : AccountRepository {

  override suspend fun profile(): AccountProfile = api.user(requireToken()).toProfile()

  override suspend fun signOut() = session.end()

  override suspend fun requestPasswordReset(login: String) = api.requestPasswordReset(login)

  override suspend fun requestAccountDeletion() = api.sendAccountDeletionCode(requireToken())

  override suspend fun confirmAccountDeletion(code: String) {
    api.confirmAccountDeletion(requireToken(), code)
    session.end()
  }

  private fun requireToken(): String =
    checkNotNull(session.state.value.tokenOrNull) { "account action attempted without a session" }
}

private fun UserDto.toProfile() = AccountProfile(name = username, email = email)