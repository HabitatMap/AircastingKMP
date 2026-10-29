package pl.llp.aircasting.settings.help

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.getOrThrow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HelpViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun loads_the_links_for_the_given_locale() = runTest {
    val repo = FakeHelpRepository(Result.success(LINKS))
    val vm = HelpViewModel(repo)
    assertEquals(HelpScreenState.Loading, vm.state.value)

    vm.load("fr"); testScheduler.advanceUntilIdle()

    assertEquals(HelpScreenState.Content(LINKS), vm.state.value)
    assertEquals(listOf("fr"), repo.locales)
  }

  @Test
  fun a_failed_load_is_Error() = runTest {
    val vm = HelpViewModel(FakeHelpRepository(Result.failure(RuntimeException("offline"))))

    vm.load("en-US"); testScheduler.advanceUntilIdle()

    assertEquals(HelpScreenState.Error, vm.state.value)
  }

  @Test
  fun retrying_after_an_error_shows_the_links() = runTest {
    val repo = FakeHelpRepository(Result.failure(RuntimeException("offline")))
    val vm = HelpViewModel(repo)
    vm.load("en-US"); testScheduler.advanceUntilIdle()

    repo.result = Result.success(LINKS)
    vm.load("en-US"); testScheduler.advanceUntilIdle()

    assertEquals(HelpScreenState.Content(LINKS), vm.state.value)
  }
}

private val LINKS = HelpLinks(
  help = listOf(HelpLink("FAQ", "https://www.habitatmap.org/airbeam/faq")),
  actions = listOf(HelpLink("Air Quality 101", "https://aircastingactions.org/air-quality-101/")),
)

private class FakeHelpRepository(var result: Result<HelpLinks>) : HelpRepository {
  val locales = mutableListOf<String>()
  override suspend fun links(locale: String): HelpLinks {
    locales += locale
    return result.getOrThrow()
  }
}