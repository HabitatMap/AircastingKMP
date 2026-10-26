package pl.llp.aircasting.settings.account

sealed interface AccountScreenState {
  data object Loading : AccountScreenState
  data class Content(
    val profile: AccountProfile,
    val deletion: Deletion = Deletion.None,
    val edit: ProfileEdit = ProfileEdit.None,
    val password: PasswordChange = PasswordChange.None,
  ) : AccountScreenState
  data object Error : AccountScreenState
}

sealed interface Deletion {
  data object None : Deletion
  data object Confirming : Deletion
  data class AwaitingCode(val isRejected: Boolean = false) : Deletion
}

enum class ProfileField { Email, Username }

sealed interface ProfileEdit {
  data object None : ProfileEdit
  data class Open(val field: ProfileField, val error: EditError? = null) : ProfileEdit
}

/** In form order: the dialog renders [entries] top to bottom. */
enum class PasswordField(val wireName: String) {
  Current("current_password"),
  New("password"),
  Confirmation("password_confirmation"),
}

sealed interface PasswordChange {
  data object None : PasswordChange
  data class Open(val errors: Map<PasswordField, EditError> = emptyMap()) : PasswordChange
}

/** Rails' own code for a `confirmation` validation, so a local mismatch reads like the backend's. */
internal val PasswordMismatch = FieldError("confirmation", "doesn't match Password")

sealed interface EditError {
  /** The backend refused the value; show why. */
  data class Rejected(val reason: FieldError) : EditError
  /** The backend refused the update without naming a field; its message is English-only prose. */
  data object Refused : EditError
  /** Network down, 5xx, unreadable body — nothing about the value itself. */
  data object Unexpected : EditError
}

internal fun AccountProfile.valueOf(field: ProfileField) = when (field) {
  ProfileField.Email -> email
  ProfileField.Username -> name
}