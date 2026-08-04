package pl.llp.aircasting.settings.account

import pl.llp.aircasting.data.auth.AuthTokenStore
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
  private val tokens: AuthTokenStore,
) : AccountRepository {

  override suspend fun profile(): AccountProfile = api.user(requireToken()).toProfile()

  /**
   * Local-only for now. Legacy also synced pending sessions and wiped the DB
   * (`LogoutService.finaliseLogout`); neither exists in the rewrite yet.
   */
  override suspend fun signOut() = tokens.clear()

  override suspend fun requestPasswordReset(login: String) = api.requestPasswordReset(login)

  override suspend fun requestAccountDeletion() = api.sendAccountDeletionCode(requireToken())
  /**
   * A wrong or expired code answers **401** (`users_controller#delete_account_with_confirmation_code`),
   * which `expectSuccess = true` turns into an exception — so reaching the next line means the
   * account is gone and the token is worthless.
   */
  override suspend fun confirmAccountDeletion(code: String) {
    api.confirmAccountDeletion(requireToken(), code)
    tokens.clear()
  }

  private fun requireToken(): String =
    checkNotNull(tokens.token()) { "account action attempted without a session" }
}

private fun UserDto.toProfile() = AccountProfile(name = username, email = email)