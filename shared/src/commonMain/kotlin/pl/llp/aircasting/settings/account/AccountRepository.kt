package pl.llp.aircasting.settings.account

import kotlinx.serialization.json.JsonPrimitive
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.tokenOrNull
import pl.llp.aircasting.data.network.AccountApi
import pl.llp.aircasting.data.network.AccountUpdateResponse
import pl.llp.aircasting.data.network.ApiErrorDto
import pl.llp.aircasting.data.network.FieldErrorDto
import pl.llp.aircasting.data.network.UpdateAccountParams
import pl.llp.aircasting.data.network.UserDto

/** One reason a field was refused. [params] holds the scalar values the message interpolates (`min`, `max`). */
data class FieldError(val code: String, val message: String, val params: Map<String, String> = emptyMap())

/** The backend refused an account update (422). [fields] is keyed by wire name: `email`, `username`, `password`… */
class AccountUpdateRejected(
  val fields: Map<String, List<FieldError>>,
  override val message: String,
) : Exception(message)

interface AccountRepository {
  suspend fun profile(): AccountProfile
  suspend fun signOut()
  suspend fun requestPasswordReset(login: String)
  suspend fun requestAccountDeletion()
  suspend fun confirmAccountDeletion(code: String)
  suspend fun updateProfile(name: String? = null, email: String? = null): AccountProfile
  suspend fun changePassword(current: String, new: String)
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

  override suspend fun updateProfile(name: String?, email: String?): AccountProfile =
    update(UpdateAccountParams(username = name, email = email)).toProfile()

  override suspend fun changePassword(current: String, new: String) {
    update(UpdateAccountParams(password = new, passwordConfirmation = new, currentPassword = current))
  }

  private suspend fun update(params: UpdateAccountParams): UserDto =
    when (val response = api.updateUser(requireToken(), params)) {
      is AccountUpdateResponse.Updated -> response.user
      is AccountUpdateResponse.Invalid -> throw response.error.toRejection()
      is AccountUpdateResponse.Failed -> error("account update failed: HTTP ${response.status}")
    }
  private fun ApiErrorDto.toRejection() = AccountUpdateRejected(
    fields = fields.mapValues { (_, errors) -> errors.map { it.toFieldError() } },
    message = message,
  )

  private fun FieldErrorDto.toFieldError() = FieldError(
    code = code,
    message = message,
    // Scalars only: `min`/`max` are what profile fields can fail on. A list param (`allowed`)
    // would need its own type, and nothing we edit can produce one.
    params = params.orEmpty().mapNotNull { (key, value) -> (value as? JsonPrimitive)?.let { key to it.content } }.toMap(),
  )

  private fun requireToken(): String =
    checkNotNull(session.state.value.tokenOrNull) { "account action attempted without a session" }
}

private fun UserDto.toProfile() = AccountProfile(name = username, email = email)