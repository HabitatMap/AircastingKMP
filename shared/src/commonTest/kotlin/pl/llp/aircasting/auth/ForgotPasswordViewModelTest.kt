package pl.llp.aircasting.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.settings.account.AccountProfile
import pl.llp.aircasting.settings.account.AccountRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

  @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest fun tearDown() = Dispatchers.resetMain()

  @Test
  fun a_blank_email_cannot_be_submitted() {
    val vm = ForgotPasswordViewModel(FakeAccountRepository())

    assertFalse(vm.state.value.canSubmit)
  }

  @Test
  fun a_sent_link_is_reported_and_the_address_is_trimmed() = runTest {
    val repo = FakeAccountRepository()
    val vm = ForgotPasswordViewModel(repo)
    vm.onEmailChange("  ada@example.com ")
    vm.submit()
    testScheduler.advanceUntilIdle()

    assertEquals(ForgotPasswordState.Status.Sent, vm.state.value.status)
    assertEquals("ada@example.com", repo.resetFor)
  }

  @Test
  fun a_failed_request_is_reported_without_throwing() = runTest {
    val repo = FakeAccountRepository().apply { fail = true }
    val vm = ForgotPasswordViewModel(repo)
    vm.onEmailChange("ada@example.com")

    vm.submit()
    testScheduler.advanceUntilIdle()

    assertEquals(ForgotPasswordState.Status.Failed, vm.state.value.status)
  }
  @Test
  fun editing_the_address_clears_a_previous_verdict() = runTest {
    val repo = FakeAccountRepository().apply { fail = true }
    val vm = ForgotPasswordViewModel(repo)
    vm.onEmailChange("typo@example.com")
    vm.submit()
    testScheduler.advanceUntilIdle()

    vm.onEmailChange("ada@example.com")

    assertEquals(ForgotPasswordState.Status.Editing, vm.state.value.status)
  }
  @Test
  fun an_in_flight_request_swallows_a_second_tap() = runTest {
    val repo = FakeAccountRepository().apply { gate = CompletableDeferred() }
    val vm = ForgotPasswordViewModel(repo)
    vm.onEmailChange("ada@example.com")

    vm.submit()
    testScheduler.advanceUntilIdle()

    assertEquals(ForgotPasswordState.Status.Sending, vm.state.value.status)
    assertFalse(vm.state.value.canSubmit)

    vm.submit()
    repo.gate!!.complete(Unit)
    testScheduler.advanceUntilIdle()

    assertEquals(1, repo.resetCalls)
  }
}

private class FakeAccountRepository : AccountRepository {
  var fail = false
  var gate: CompletableDeferred<Unit>? = null
  var resetFor: String? = null
  var resetCalls = 0

  override suspend fun requestPasswordReset(login: String) {
    resetCalls++
    resetFor = login
    gate?.await()
    if (fail) throw RuntimeException("network down")
  }
  override suspend fun profile() = AccountProfile(name = "ada", email = "ada@example.com")
  override suspend fun signOut() = Unit
  override suspend fun requestAccountDeletion() = Unit
  override suspend fun confirmAccountDeletion(code: String) = Unit
}