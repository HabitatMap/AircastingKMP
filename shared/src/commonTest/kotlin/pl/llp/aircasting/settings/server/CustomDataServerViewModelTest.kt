package pl.llp.aircasting.settings.server

import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.data.auth.InMemoryAuthTokenStore
import pl.llp.aircasting.data.network.ServerProbe
import pl.llp.aircasting.settings.app.AppPreferences
import pl.llp.aircasting.settings.app.FakeAppSettingsRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CustomDataServerViewModelTest {

  @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest fun tearDown() = Dispatchers.resetMain()

  @Test
  fun `next walks intro to a form pre-filled with the server in use`() = runTest {
    val repo = FakeAppSettingsRepository(AppPreferences(dataServerUrl = "https://my.server:8080"))
    val vm = viewModel(repo)

    assertEquals(CustomServerStep.Intro, vm.step.value)
    vm.next()

    assertEquals(
      CustomServerStep.Form(CustomServerForm(host = "https://my.server", port = "8080")),
      vm.step.value,
    )
  }
  @Test
  fun `next on an incomplete form does nothing`() = runTest {
    // The Next button is disabled in the UI, but the ViewModel is the one that must not probe
    // a URL that does not exist — a disabled button is a hint, not an invariant.
    val vm = viewModel()
    vm.next()
    vm.edit(CustomServerForm(host = "air casting.org"))

    vm.next(); advanceUntilIdle()

    assertIs<CustomServerStep.Form>(vm.step.value)
  }
  @Test
  fun `a reachable server ends on Verified`() = runTest {
    val vm = viewModel(probe = { true })
    vm.next()
    vm.edit(CustomServerForm(host = "https://my.server"))

    vm.next(); advanceUntilIdle()

    assertEquals(CustomServerStep.Verified("https://my.server"), vm.step.value)
  }

  @Test
  fun `an unreachable server ends on Failed, and nothing is saved`() = runTest {
    // "We'll test the connection before saving anything" — the design says it out loud.
    val repo = FakeAppSettingsRepository()
    val vm = viewModel(repo, probe = { false })
    vm.next()
    vm.edit(CustomServerForm(host = "https://nope.invalid"))

    vm.next(); advanceUntilIdle()

    assertEquals(CustomServerStep.Failed("https://nope.invalid"), vm.step.value)
    assertNull(repo.preferences.value.dataServerUrl)
  }
  @Test
  fun `a slow probe grows a cancel affordance without leaving the testing state`() = runTest {
    val vm = viewModel(probe = { kotlinx.coroutines.delay(60.seconds); true })
    vm.next()
    vm.edit(CustomServerForm(host = "https://slow.server"))
    vm.next()

    advanceTimeBy(TooLongThreshold - 1.seconds)
    assertEquals(CustomServerStep.Testing("https://slow.server"), vm.step.value)

    advanceTimeBy(2.seconds)
    assertEquals(CustomServerStep.Testing("https://slow.server", tookTooLong = true), vm.step.value)
  }
  @Test
  fun `cancelling a probe returns to the form with the typed values intact`() = runTest {
    // Losing what they typed after a 30-second wait would be the worst possible moment for it.
    val vm = viewModel(probe = { kotlinx.coroutines.delay(60.seconds); true })
    vm.next()
    vm.edit(CustomServerForm(host = "https://slow.server", port = "8080"))
    vm.next()
    advanceTimeBy(TooLongThreshold + 1.seconds)

    vm.cancelTest(); advanceUntilIdle()

    assertEquals(
      CustomServerStep.Form(CustomServerForm(host = "https://slow.server", port = "8080")),
      vm.step.value,
    )
  }

  @Test
  fun `saving writes the url and drops the session`() = runTest {
    // "You'll be logged out to apply the change": the token belongs to the *old* server and is
    // meaningless — worse, replayable — against the new one, so it must go with the switch.
    val repo = FakeAppSettingsRepository()
    val tokens = InMemoryAuthTokenStore(initial = "old-server-token")
    val vm = viewModel(repo, tokens = tokens, probe = { true })
    vm.next()
    vm.edit(CustomServerForm(host = "https://my.server"))
    vm.next(); advanceUntilIdle()

    vm.save(); advanceUntilIdle()

    assertEquals("https://my.server", repo.preferences.value.dataServerUrl)
    assertNull(tokens.token())
  }

  @Test
  fun `going back from Failed reopens the form for editing`() = runTest {
    val vm = viewModel(probe = { false })
    vm.next()
    vm.edit(CustomServerForm(host = "https://typo.invalid"))
    vm.next(); advanceUntilIdle()

    vm.back()

    assertEquals(CustomServerStep.Form(CustomServerForm(host = "https://typo.invalid")), vm.step.value)
  }

  @Test
  fun `going back from Verified reopens the form for editing`() = runTest {
    val vm = viewModel(probe = { true })
    vm.next()
    vm.edit(CustomServerForm(host = "https://my.server", port = "8080"))
    vm.next(); advanceUntilIdle()
    assertEquals(CustomServerStep.Verified("https://my.server:8080"), vm.step.value)

    vm.back()

    assertEquals(
      CustomServerStep.Form(CustomServerForm(host = "https://my.server", port = "8080")),
      vm.step.value,
    )
  }

  @Test
  fun `choosing the official server clears the custom one and logs out`() = runTest {
    val repo = FakeAppSettingsRepository(AppPreferences(dataServerUrl = "https://my.server"))
    val tokens = InMemoryAuthTokenStore(initial = "token")
    val vm = viewModel(repo, tokens = tokens)

    vm.useOfficialServer(); advanceUntilIdle()

    assertNull(repo.preferences.value.dataServerUrl)
    assertNull(tokens.token())
  }

  private fun viewModel(
    repo: FakeAppSettingsRepository = FakeAppSettingsRepository(),
    tokens: InMemoryAuthTokenStore = InMemoryAuthTokenStore(),
    probe: ServerProbe = ServerProbe { true },
  ) = CustomDataServerViewModel(repo, probe, tokens)
}