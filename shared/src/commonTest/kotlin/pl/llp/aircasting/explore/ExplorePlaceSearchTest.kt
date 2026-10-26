package pl.llp.aircasting.explore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.home.FakeLocationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorePlaceSearchTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val places = FakePlaceSearch(mapOf(KRAKOW.id to KRAKOW_CENTRE)) { query ->
    if (query.startsWith("Kra")) listOf(KRAKOW) else emptyList()
  }

  /** Built inside the test, not as a property: viewModelScope must see the Main set in [setUp]. */
  private fun TestScope.subscribedViewModel(): ExploreViewModel {
    val vm = ExploreViewModel(FakeExploreMapRepository(), FakeThresholdsRepository(emptyList()), FakeLocationProvider(null), places)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
    return vm
  }

  @Test
  fun looks_up_only_what_was_typed_last_once_typing_pauses() = runTest {
    val vm = subscribedViewModel()

    vm.searchPlaces("Kr")
    advanceTimeBy(100) // still typing
    vm.searchPlaces("Krak")
    advanceUntilIdle()

    assertEquals(listOf("Krak"), places.queries)
    assertEquals(listOf(KRAKOW), vm.state.value.suggestions)
  }

  @Test
  fun a_blank_query_clears_the_list_without_a_lookup() = runTest {
    val vm = subscribedViewModel()
    vm.searchPlaces("Krak")
    advanceUntilIdle()

    vm.searchPlaces("  ")
    advanceUntilIdle()

    assertEquals(listOf("Krak"), places.queries)
    assertEquals(emptyList(), vm.state.value.suggestions)
  }

  @Test
  fun picking_a_suggestion_flies_the_map_there_and_closes_the_list() = runTest {
    val vm = subscribedViewModel()
    vm.searchPlaces("Krak")
    advanceUntilIdle()

    vm.selectPlace(KRAKOW)
    advanceUntilIdle()

    assertEquals(KRAKOW_CENTRE, vm.state.value.flyTo)
    assertEquals(emptyList(), vm.state.value.suggestions)
  }

  @Test
  fun picking_the_same_place_again_flies_again() = runTest {
    val vm = subscribedViewModel()
    vm.selectPlace(KRAKOW)
    advanceUntilIdle()
    vm.onFlownTo()
    advanceUntilIdle()
    assertNull(vm.state.value.flyTo)

    vm.selectPlace(KRAKOW)
    advanceUntilIdle()

    assertEquals(KRAKOW_CENTRE, vm.state.value.flyTo)
  }
  @Test
  fun a_place_without_a_location_leaves_the_map_where_it_is() = runTest {
    val vm = subscribedViewModel()

    vm.selectPlace(PlaceSuggestion("unknown", "Nowhere", ""))
    advanceUntilIdle()

    assertNull(vm.state.value.flyTo)
  }

  private companion object {
    val KRAKOW = PlaceSuggestion("krk", "Kraków", "Poland")
    val KRAKOW_CENTRE = GeoLocation(50.06, 19.94)
  }
}