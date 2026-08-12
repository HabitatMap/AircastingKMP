package pl.llp.aircasting.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.data.auth.AuthState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
  @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest fun tearDown() = Dispatchers.resetMain()

  private fun signInReady(vm: AuthViewModel) {
    vm.onLoginChange("ada")
    vm.onPasswordChange("s3cret")
  }

  @Test
  fun an_empty_form_cannot_be_submitted() {
    val vm = AuthViewModel(FakeAuthRepository())

    assertEquals(AuthMode.SignIn, vm.form.value.mode)
    assertFalse(vm.form.value.canSubmit)
  }
  @Test
  fun sign_up_needs_all_three_fields_where_sign_in_needs_two() {
    val vm = AuthViewModel(FakeAuthRepository())
    vm.switchTo(AuthMode.SignUp)
    vm.onEmailChange("ada@example.com")
    vm.onPasswordChange("s3cret")

    assertFalse(vm.form.value.canSubmit)

    vm.onUsernameChange("ada")
    assertTrue(vm.form.value.canSubmit)
  }

  @Test
  fun a_successful_sign_in_flips_the_session() = runTest {
    val repo = FakeAuthRepository()
    val vm = AuthViewModel(repo)
    signInReady(vm)

    vm.submit()
    testScheduler.advanceUntilIdle()

    assertEquals(AuthState.SignedIn("tok"), vm.session.value)
    assertFalse(vm.form.value.submitting)
    assertNull(vm.form.value.failure)
  }

  @Test
  fun an_in_flight_submit_disables_the_form_and_swallows_a_second_tap() = runTest {
    val repo = FakeAuthRepository().apply { gate = CompletableDeferred() }
    val vm = AuthViewModel(repo)
    signInReady(vm)

    vm.submit()
    testScheduler.advanceUntilIdle()

    assertTrue(vm.form.value.submitting)
    assertFalse(vm.form.value.canSubmit)

    vm.submit()
    repo.gate!!.complete(Unit)
    testScheduler.advanceUntilIdle()

    assertEquals(1, repo.calls)
    assertFalse(vm.form.value.submitting)
  }

  @Test
  fun bad_credentials_land_as_a_form_level_error() = runTest {
    val repo = FakeAuthRepository().apply { result = Result.failure(AuthFailure.InvalidCredentials) }
    val vm = AuthViewModel(repo)
    signInReady(vm)

    vm.submit()
    testScheduler.advanceUntilIdle()

    assertEquals(AuthFailure.InvalidCredentials, vm.form.value.formError)
    assertNull(vm.form.value.passwordError)
    assertFalse(vm.form.value.submitting)
  }

  @Test
  fun a_rejected_sign_up_marks_the_individual_fields() = runTest {
    val repo = FakeAuthRepository().apply {
      result = Result.failure(
        AuthFailure.Invalid(email = "has already been taken", username = null, password = null),
      )
    }
    val vm = AuthViewModel(repo)
    vm.switchTo(AuthMode.SignUp)
    vm.onEmailChange("ada@example.com")
    vm.onUsernameChange("ada")
    vm.onPasswordChange("s3cret")

    vm.submit()
    testScheduler.advanceUntilIdle()

    assertEquals("has already been taken", vm.form.value.emailError)
    assertNull(vm.form.value.usernameError)
    assertNull(vm.form.value.formError)
  }

  @Test
  fun typing_retires_the_previous_error() = runTest {
    val repo = FakeAuthRepository().apply { result = Result.failure(AuthFailure.InvalidCredentials) }
    val vm = AuthViewModel(repo)
    signInReady(vm)
    vm.submit()
    testScheduler.advanceUntilIdle()

    vm.onPasswordChange("s3cret2")

    assertNull(vm.form.value.failure)
  }

  @Test
  fun switching_tab_drops_errors_raised_by_the_other_form() = runTest {
    val repo = FakeAuthRepository().apply { result = Result.failure(AuthFailure.InvalidCredentials) }
    val vm = AuthViewModel(repo)
    signInReady(vm)
    vm.submit()
    testScheduler.advanceUntilIdle()

    vm.switchTo(AuthMode.SignUp)

    assertNull(vm.form.value.failure)
    assertEquals("s3cret", vm.form.value.password)
  }

  @Test
  fun identifiers_are_trimmed_and_the_password_is_sent_exactly_as_typed() = runTest {
    val repo = FakeAuthRepository()
    val vm = AuthViewModel(repo)
    vm.onLoginChange("  ada  ")
    vm.onPasswordChange(" s3cret ")

    vm.submit()
    testScheduler.advanceUntilIdle()

    assertEquals("signIn|ada| s3cret ", repo.lastCall)
  }
}

private class FakeAuthRepository : AuthRepository {
  private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
  override val state: StateFlow<AuthState> = _state

  var result: Result<Unit> = Result.success(Unit)
  var gate: CompletableDeferred<Unit>? = null
  var calls = 0
  var lastCall: String? = null

  override suspend fun signIn(login: String, password: String): Result<Unit> {
    lastCall = "signIn|$login|$password"
    return record()
  }

  override suspend fun signUp(email: String, username: String, password: String): Result<Unit> {
    lastCall = "signUp|$email|$username|$password"
    return record()
  }
  override fun signOut() { _state.value = AuthState.SignedOut }

  private suspend fun record(): Result<Unit> {
    calls++
    gate?.await()
    if (result.isSuccess) _state.value = AuthState.SignedIn("tok")
    return result
  }
}