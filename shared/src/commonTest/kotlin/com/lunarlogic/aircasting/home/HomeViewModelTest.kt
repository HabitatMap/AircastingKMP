package com.lunarlogic.aircasting.home

import app.cash.turbine.test
import com.lunarlogic.aircasting.domain.GeoLocation
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val located = HomeUiState(
    airQuality = HomeUiState.AirQuality.NoReadings,
    nearby = emptyList(),
  )

  @Test
  fun starts_in_Loading_before_the_first_load_completes() = runTest {
    val vm = HomeViewModel(FakeHomeRepository(Result.success(located)), FakeLocationProvider(null))

    assertEquals(HomeScreenState.Loading, vm.state.value)
  }

  @Test
  fun emits_Loading_then_Content_when_the_load_succeeds() = runTest {
    val vm = HomeViewModel(FakeHomeRepository(Result.success(located)), FakeLocationProvider(HERE))

    vm.state.test {
      assertEquals(HomeScreenState.Loading, awaitItem())
      assertEquals(HomeScreenState.Content(located), awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun feeds_the_providers_location_into_the_repository() = runTest {
    val repo = FakeHomeRepository(Result.success(located))
    HomeViewModel(repo, FakeLocationProvider(HERE))

    testScheduler.advanceUntilIdle()

    assertEquals(HERE, repo.receivedLocation)
  }

  @Test
  fun emits_Error_when_the_repository_throws() = runTest {
    val vm = HomeViewModel(
      FakeHomeRepository(Result.failure(RuntimeException("network down"))),
      FakeLocationProvider(HERE),
    )

    vm.state.test {
      assertEquals(HomeScreenState.Loading, awaitItem())
      assertIs<HomeScreenState.Error>(awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun refresh_recovers_from_Error_to_Content() = runTest {
    val repo = FakeHomeRepository(Result.failure(RuntimeException("boom")))
    val vm = HomeViewModel(repo, FakeLocationProvider(HERE))
    testScheduler.advanceUntilIdle()
    assertIs<HomeScreenState.Error>(vm.state.value)

    repo.result = Result.success(located)
    vm.refresh()
    testScheduler.advanceUntilIdle()

    assertEquals(HomeScreenState.Content(located), vm.state.value)
  }
  private companion object { val HERE = GeoLocation(40.78, -73.96) }
}


private class FakeLocationProvider(private val location: GeoLocation?) : LocationProvider {
  override suspend fun current() = location
}

private class FakeHomeRepository(var result: Result<HomeUiState>) : HomeRepository {
  var receivedLocation: GeoLocation? = null
  override suspend fun load(userLocation: GeoLocation?): HomeUiState {
    receivedLocation = userLocation
    return result.getOrThrow()
  }
}