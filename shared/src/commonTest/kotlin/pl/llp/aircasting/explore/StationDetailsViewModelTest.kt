package pl.llp.aircasting.explore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.Pollutant
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import kotlin.getOrThrow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StationDetailsViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  @Test
  fun loads_the_station_by_uuid_with_the_first_stream_selected() = runTest {
    val repo = FakeStationDetailsRepository(Result.success(STATION))
    val vm = StationDetailsViewModel("u1", repo)
    assertEquals(StationDetailsState.Loading, vm.state.value)

    advanceUntilIdle()

    assertEquals(StationDetailsState.Content(STATION, selectedStreamId = 1), vm.state.value)
    assertEquals(listOf("u1"), repo.uuids)
  }

  @Test
  fun a_failed_load_is_Error() = runTest {
    val vm = StationDetailsViewModel("u1", FakeStationDetailsRepository(Result.failure(RuntimeException("offline"))))

    advanceUntilIdle()

    assertEquals(StationDetailsState.Error, vm.state.value)
  }

  @Test
  fun retrying_after_an_error_shows_the_station() = runTest {
    val repo = FakeStationDetailsRepository(Result.failure(RuntimeException("offline")))
    val vm = StationDetailsViewModel("u1", repo)
    advanceUntilIdle()

    repo.result = Result.success(STATION)
    vm.load(); advanceUntilIdle()

    assertEquals(StationDetailsState.Content(STATION, selectedStreamId = 1), vm.state.value)
  }

  @Test
  fun selecting_a_chip_switches_the_shown_stream() = runTest {
    val vm = StationDetailsViewModel("u1", FakeStationDetailsRepository(Result.success(STATION)))
    advanceUntilIdle()

    vm.selectStream(2)

    assertEquals(Pollutant.PM25.unitSymbol, (vm.state.value as StationDetailsState.Content).selected?.unitSymbol)
  }
}

private val PM1 = StationStream(
  id = 1, sensorName = "AirBeam-PM1", unitSymbol = "µg/m³", value = 8, min24h = 3, max24h = 10,
  measuredAt = Instant.fromEpochMilliseconds(1793282400000),
)
private val PM25 = PM1.copy(id = 2, sensorName = "AirBeam-PM2.5", value = 12)
private val STATION = StationDetails(
  source = DataSource.AirBeam, title = "Garden", provider = "jane", deviceModel = "AirBeamMini",
  streams = listOf(PM1, PM25),
)