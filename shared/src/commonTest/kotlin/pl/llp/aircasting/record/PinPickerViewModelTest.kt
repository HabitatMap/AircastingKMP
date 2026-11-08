package pl.llp.aircasting.record

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.explore.FakePlaceSearch
import pl.llp.aircasting.explore.PlaceSuggestion
import pl.llp.aircasting.recording.PhoneLocation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** "Where's your AirBeam?": the map goes where the user asks; the pin follows the map. */
@OptIn(ExperimentalCoroutinesApi::class)
class PinPickerViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val places = FakePlaceSearch(mapOf(WAWEL.id to WAWEL_SPOT)) { listOf(WAWEL) }

  private fun TestScope.subscribedViewModel(fix: GeoLocation? = null): PinPickerViewModel {
    val vm = PinPickerViewModel(places, StillPhone(fix))
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
    return vm
  }

  @Test
  fun typing_an_address_suggests_places() = runTest {
    val vm = subscribedViewModel()

    vm.search("Wawel")
    advanceUntilIdle()

    assertEquals(listOf(WAWEL), vm.state.value.suggestions)
  }

  @Test
  fun picking_a_place_flies_the_map_there_and_closes_the_list() = runTest {
    val vm = subscribedViewModel()
    vm.search("Wawel")
    advanceUntilIdle()

    vm.selectPlace(WAWEL)
    advanceUntilIdle()

    assertEquals(WAWEL_SPOT, vm.state.value.flyTo)
    assertEquals(emptyList(), vm.state.value.suggestions)
  }

  @Test
  fun my_location_flies_to_the_phone_until_the_map_gets_there() = runTest {
    val vm = subscribedViewModel(fix = HOME)

    vm.locateMe()
    advanceUntilIdle()
    assertEquals(HOME, vm.state.value.flyTo)

    vm.onFlownTo()
    advanceUntilIdle()
    assertNull(vm.state.value.flyTo)
  }

  @Test
  fun my_location_without_a_fix_stays_put() = runTest {
    val vm = subscribedViewModel(fix = null)

    vm.locateMe()
    advanceUntilIdle()

    assertNull(vm.state.value.flyTo)
  }

  private companion object {
    val WAWEL = PlaceSuggestion("wawel", "Wawel", "Kraków, Poland")
    val WAWEL_SPOT = GeoLocation(50.054, 19.935)
    val HOME = GeoLocation(50.06, 19.94)
  }
}

private class StillPhone(private val fix: GeoLocation?) : PhoneLocation {
  override fun latest() = fix
  override suspend fun track() = awaitCancellation()
  override suspend fun awaitFix() = awaitCancellation()
}