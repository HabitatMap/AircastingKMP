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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun reset_password_uses_the_signed_in_email() = runTest {
    val repo = FakeAccountRepository(profile = ADAM)
    val vm = AccountViewModel(repo)
    vm.refresh(); testScheduler.advanceUntilIdle()

    vm.resetPassword(); testScheduler.advanceUntilIdle()

    assertEquals(ADAM.email, repo.resetLogin)
  }
  @Test
  fun reset_password_is_a_no_op_when_signed_out() = runTest {
    val repo = FakeAccountRepository(profile = null)
    val vm = AccountViewModel(repo)
    vm.refresh(); testScheduler.advanceUntilIdle()

    vm.resetPassword(); testScheduler.advanceUntilIdle()

    assertNull(repo.resetLogin)
  }

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

    assertEquals(content(Deletion.AwaitingCode(rejected = true)), vm.state.value)
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

    assertEquals(content(Deletion.AwaitingCode(rejected = false)), vm.state.value)
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
) : AccountRepository {
  var signOuts = 0
  var resetLogin: String? = null
  var confirmedCode: String? = null
  var deletionRequests = 0

  override suspend fun profile(): AccountProfile = failWith?.let { throw it } ?: checkNotNull(profile)
  override suspend fun signOut() { signOuts++ }
  override suspend fun requestPasswordReset(login: String) { resetLogin = login }
  override suspend fun requestAccountDeletion() {
    if (failDeletionRequest) error("network down")
    deletionRequests++
  }
  override suspend fun confirmAccountDeletion(code: String) {
    if (failConfirm) error("401 invalid code")
    confirmedCode = code
  }
}
