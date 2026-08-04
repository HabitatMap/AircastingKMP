package pl.llp.aircasting.settings.account

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
    // The endpoint takes a login, and the only one we can legitimately supply is the loaded
    // profile's. Signed out, there is nothing to send — and nothing to reset.
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
      awaitItem()                                   // navigation signal fired exactly once
      assertEquals(1, repo.signOuts)
      // State is untouched: the screen is on its way out, not repainting as signed-out.
      assertIs<AccountScreenState.Content>(vm.state.value)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private companion object {
    val ADAM = AccountProfile("adam.smith", "adam.smith.1989@mail.com")
  }
}

private class FakeAccountRepository(
  private val profile: AccountProfile? = null,
  private val failWith: Throwable? = null,
) : AccountRepository {
  var signOuts = 0
  var resetLogin: String? = null
  var confirmedCode: String? = null
  var deletionRequests = 0

  override suspend fun profile(): AccountProfile =
    failWith?.let { throw it } ?: checkNotNull(profile)
  override suspend fun signOut() { signOuts++ }
  override suspend fun requestPasswordReset(login: String) { resetLogin = login }
  override suspend fun requestAccountDeletion() { deletionRequests++ }
  override suspend fun confirmAccountDeletion(code: String) { confirmedCode = code }
}