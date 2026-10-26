package pl.llp.aircasting.settings.account

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun a_missing_session_is_an_Error_because_it_cannot_legally_happen() = runTest {
    val vm = AccountViewModel(FakeAccountRepository(failWith = IllegalStateException("no session")))
    vm.refresh(); testScheduler.advanceUntilIdle()

    assertEquals(AccountScreenState.Error, vm.state.value)
  }

  @Test
  fun sign_out_ends_the_session_rather_than_rendering_a_state() = runTest {
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = AccountViewModel(repo)
    vm.refresh(); testScheduler.advanceUntilIdle()

    vm.sessionEnded.test {
      vm.signOut()
      testScheduler.advanceUntilIdle()
      awaitItem()
      assertEquals(1, repo.signOuts)
      assertIs<AccountScreenState.Content>(vm.state.value)
      cancelAndIgnoreRemainingEvents()
    }
  }


  @Test
  fun tapping_delete_only_opens_the_confirmation_and_sends_nothing() = runTest {
    // The code email is a side effect on a real inbox. It must wait for an explicit confirm,
    // not fire on the first tap of a destructive button.
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = signedIn(repo)

    vm.startAccountDeletion(); testScheduler.advanceUntilIdle()

    assertEquals(content(Deletion.Confirming), vm.state.value)
    assertEquals(0, repo.deletionRequests)
  }

  @Test
  fun cancelling_returns_to_None() = runTest {
    val vm = signedIn(FakeAccountRepository(profile = ADAM))
    vm.startAccountDeletion(); testScheduler.advanceUntilIdle()

    vm.cancelAccountDeletion(); testScheduler.advanceUntilIdle()

    assertEquals(content(Deletion.None), vm.state.value)
  }

  @Test
  fun confirming_sends_the_code_and_opens_the_code_prompt() = runTest {
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = signedIn(repo)
    vm.startAccountDeletion(); testScheduler.advanceUntilIdle()

    vm.sendDeletionCode(); testScheduler.advanceUntilIdle()

    assertEquals(content(Deletion.AwaitingCode()), vm.state.value)
    assertEquals(1, repo.deletionRequests)
  }

  @Test
  fun a_failed_send_stays_on_the_confirmation() = runTest {
    // Asking for a code that was never emailed would strand the user: no code will ever
    // arrive, and the only way out is backing off the screen.
    val repo = FakeAccountRepository(profile = ADAM, failDeletionRequest = true)
    val vm = signedIn(repo)
    vm.startAccountDeletion(); testScheduler.advanceUntilIdle()

    vm.sendDeletionCode(); testScheduler.advanceUntilIdle()

    assertEquals(content(Deletion.Confirming), vm.state.value)
  }

  @Test
  fun a_rejected_code_keeps_the_prompt_open_and_flags_the_error() = runTest {
    // Live-verified: a wrong or >30-min-old code answers 401. Closing the prompt would lose
    // the code the user still has in their inbox.
    val repo = FakeAccountRepository(profile = ADAM, failConfirm = true)
    val vm = signedIn(repo)
    vm.startAccountDeletion()
    vm.sendDeletionCode(); testScheduler.advanceUntilIdle()

    vm.submitDeletionCode("0000"); testScheduler.advanceUntilIdle()

    assertEquals(content(Deletion.AwaitingCode(isRejected = true)), vm.state.value)
  }

  @Test
  fun resending_clears_the_rejection_and_asks_for_a_new_code() = runTest {
    val repo = FakeAccountRepository(profile = ADAM, failConfirm = true)
    val vm = signedIn(repo)
    vm.startAccountDeletion()
    vm.sendDeletionCode(); testScheduler.advanceUntilIdle()
    vm.submitDeletionCode("0000"); testScheduler.advanceUntilIdle()

    repo.failConfirm = false
    vm.sendDeletionCode(); testScheduler.advanceUntilIdle()

    assertEquals(content(Deletion.AwaitingCode(isRejected = false)), vm.state.value)
    assertEquals(2, repo.deletionRequests)
  }

  @Test
  fun a_correct_code_ends_the_session() = runTest {
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = signedIn(repo)
    vm.startAccountDeletion()
    vm.sendDeletionCode(); testScheduler.advanceUntilIdle()

    vm.sessionEnded.test {
      vm.submitDeletionCode("1234")
      testScheduler.advanceUntilIdle()
      awaitItem()
      assertEquals("1234", repo.confirmedCode)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun tapping_change_email_only_opens_the_editor() = runTest {
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = signedIn(repo)

    vm.startEdit(ProfileField.Email); testScheduler.advanceUntilIdle()

    assertEquals(AccountScreenState.Content(ADAM, edit = ProfileEdit.Open(ProfileField.Email)), vm.state.value)
    assertEquals(emptyList(), repo.updates)
  }

  @Test
  fun saving_an_email_patches_only_the_email_and_shows_the_saved_profile() = runTest {
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = signedIn(repo)
    vm.startEdit(ProfileField.Email)

    vm.saveEdit("  adam@new-mail.com "); testScheduler.advanceUntilIdle()

    assertEquals(listOf<Pair<String?, String?>>(null to "adam@new-mail.com"), repo.updates)
    assertEquals(AccountScreenState.Content(ADAM.copy(email = "adam@new-mail.com")), vm.state.value)
  }

  @Test
  fun saving_a_username_patches_only_the_username() = runTest {
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = signedIn(repo)
    vm.startEdit(ProfileField.Username)

    vm.saveEdit("adam.s"); testScheduler.advanceUntilIdle()

    assertEquals(listOf<Pair<String?, String?>>("adam.s" to null), repo.updates)
    assertEquals(AccountScreenState.Content(ADAM.copy(name = "adam.s")), vm.state.value)
  }

  @Test
  fun a_rejected_email_shows_the_backends_reason_for_that_field() = runTest {
    val taken = FieldError("taken", "has already been taken")
    val repo = FakeAccountRepository(
      profile = ADAM,
      updateFailure = AccountUpdateRejected(mapOf("email" to listOf(taken)), "Account could not be updated"),
    )
    val vm = signedIn(repo)
    vm.startEdit(ProfileField.Email)

    vm.saveEdit("taken@mail.com"); testScheduler.advanceUntilIdle()

    assertEquals(
      AccountScreenState.Content(ADAM, edit = ProfileEdit.Open(ProfileField.Email, EditError.Rejected(taken))),
      vm.state.value,
    )
  }

  @Test
  fun a_rejection_with_nothing_for_the_field_is_Refused() = runTest {
    // The top-level message is English-only prose; the UI shows its own translated text instead.
    val repo = FakeAccountRepository(
      profile = ADAM,
      updateFailure = AccountUpdateRejected(emptyMap(), "Nothing to update"),
    )
    val vm = signedIn(repo)
    vm.startEdit(ProfileField.Username)

    vm.saveEdit("adam.s"); testScheduler.advanceUntilIdle()

    assertEquals(
      ProfileEdit.Open(ProfileField.Username, EditError.Refused),
      (vm.state.value as AccountScreenState.Content).edit,
    )
  }

  @Test
  fun a_network_failure_is_unexpected_and_keeps_the_editor_open() = runTest {
    val repo = FakeAccountRepository(profile = ADAM, updateFailure = RuntimeException("offline"))
    val vm = signedIn(repo)
    vm.startEdit(ProfileField.Email)

    vm.saveEdit("adam@new-mail.com"); testScheduler.advanceUntilIdle()

    assertEquals(
      ProfileEdit.Open(ProfileField.Email, EditError.Unexpected),
      (vm.state.value as AccountScreenState.Content).edit,
    )
  }

  @Test
  fun saving_an_unchanged_value_closes_the_editor_without_a_request() = runTest {
    // The backend 422s an update with nothing to change.
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = signedIn(repo)
    vm.startEdit(ProfileField.Email)

    vm.saveEdit(ADAM.email); testScheduler.advanceUntilIdle()

    assertEquals(emptyList(), repo.updates)
    assertEquals(AccountScreenState.Content(ADAM), vm.state.value)
  }


  @Test
  fun tapping_change_password_only_opens_the_dialog() = runTest {
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = signedIn(repo)

    vm.startPasswordChange(); testScheduler.advanceUntilIdle()

    assertEquals(AccountScreenState.Content(ADAM, password = PasswordChange.Open()), vm.state.value)
    assertEquals(emptyList(), repo.passwordChanges)
  }

  @Test
  fun a_mismatched_confirmation_is_flagged_without_a_request() = runTest {
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = signedIn(repo)
    vm.startPasswordChange()

    vm.changePassword(current = "old-pass", new = "new-pass", confirmation = "new-pas"); testScheduler.advanceUntilIdle()

    assertEquals(emptyList(), repo.passwordChanges)
    assertEquals(
      PasswordChange.Open(mapOf(PasswordField.Confirmation to EditError.Rejected(PasswordMismatch))),
      (vm.state.value as AccountScreenState.Content).password,
    )
  }

  @Test
  fun a_confirmed_change_sends_current_and_new_and_closes_the_dialog() = runTest {
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = signedIn(repo)
    vm.startPasswordChange()

    vm.changePassword(current = "old-pass", new = "new-pass", confirmation = "new-pass"); testScheduler.advanceUntilIdle()

    assertEquals(listOf("old-pass" to "new-pass"), repo.passwordChanges)
    assertEquals(AccountScreenState.Content(ADAM), vm.state.value)
  }

  @Test
  fun a_wrong_current_password_is_shown_on_the_current_field() = runTest {
    val invalid = FieldError("invalid", "is invalid")
    val repo = FakeAccountRepository(
      profile = ADAM,
      updateFailure = AccountUpdateRejected(mapOf("current_password" to listOf(invalid)), "Account could not be updated"),
    )
    val vm = signedIn(repo)
    vm.startPasswordChange()

    vm.changePassword(current = "wrong", new = "new-pass", confirmation = "new-pass"); testScheduler.advanceUntilIdle()

    assertEquals(
      PasswordChange.Open(mapOf(PasswordField.Current to EditError.Rejected(invalid))),
      (vm.state.value as AccountScreenState.Content).password,
    )
  }

  @Test
  fun a_password_rejection_naming_no_field_is_Refused_on_the_last_field() = runTest {
    val repo = FakeAccountRepository(
      profile = ADAM,
      updateFailure = AccountUpdateRejected(emptyMap(), "Account could not be updated"),
    )
    val vm = signedIn(repo)
    vm.startPasswordChange()

    vm.changePassword(current = "old-pass", new = "new-pass", confirmation = "new-pass"); testScheduler.advanceUntilIdle()

    assertEquals(
      PasswordChange.Open(mapOf(PasswordField.Confirmation to EditError.Refused)),
      (vm.state.value as AccountScreenState.Content).password,
    )
  }

  @Test
  fun a_network_failure_keeps_the_dialog_open_with_an_unexpected_error() = runTest {
    val repo = FakeAccountRepository(profile = ADAM, updateFailure = RuntimeException("offline"))
    val vm = signedIn(repo)
    vm.startPasswordChange()

    vm.changePassword(current = "old-pass", new = "new-pass", confirmation = "new-pass"); testScheduler.advanceUntilIdle()

    assertEquals(
      PasswordChange.Open(mapOf(PasswordField.Confirmation to EditError.Unexpected)),
      (vm.state.value as AccountScreenState.Content).password,
    )
  }

  private fun TestScope.signedIn(repo: FakeAccountRepository) =
    AccountViewModel(repo).also { it.refresh(); testScheduler.advanceUntilIdle() }

  private fun content(deletion: Deletion) = AccountScreenState.Content(ADAM, deletion)


  private companion object {
    val ADAM = AccountProfile("adam.smith", "adam.smith.1989@mail.com")
  }
}

private class FakeAccountRepository(
  private val profile: AccountProfile? = null,
  private val failWith: Throwable? = null,
  private val failDeletionRequest: Boolean = false,
  var failConfirm: Boolean = false,
  private val updateFailure: Throwable? = null,
) : AccountRepository {
  var signOuts = 0
  var confirmedCode: String? = null
  var deletionRequests = 0
  /** Each updateProfile call as (name, email). */
  val updates = mutableListOf<Pair<String?, String?>>()
  /** Each changePassword call as (current, new). */
  val passwordChanges = mutableListOf<Pair<String, String>>()

  override suspend fun profile(): AccountProfile = failWith?.let { throw it } ?: checkNotNull(profile)
  override suspend fun signOut() { signOuts++ }
  override suspend fun requestPasswordReset(login: String) = Unit
  override suspend fun requestAccountDeletion() {
    if (failDeletionRequest) error("network down")
    deletionRequests++
  }
  override suspend fun confirmAccountDeletion(code: String) {
    if (failConfirm) error("401 invalid code")
    confirmedCode = code
  }
  override suspend fun updateProfile(name: String?, email: String?): AccountProfile {
    updateFailure?.let { throw it }
    updates += name to email
    // Echo like the backend does: the saved user comes back in the response.
    val current = checkNotNull(profile)
    return current.copy(name = name ?: current.name, email = email ?: current.email)
  }
  override suspend fun changePassword(current: String, new: String) {
    updateFailure?.let { throw it }
    passwordChanges += current to new
  }
}
