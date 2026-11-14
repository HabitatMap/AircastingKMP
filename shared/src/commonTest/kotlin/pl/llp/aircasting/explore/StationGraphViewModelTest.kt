package pl.llp.aircasting.explore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.ui.chart.ChartPoint
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
@OptIn(ExperimentalCoroutinesApi::class)
class StationGraphViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  // The sort runs off the main thread; in tests it shares the test scheduler so advanceUntilIdle covers it.
  private fun TestScope.viewModel(repo: FakeStationMeasurementsRepository, stream: StationStream = STREAM) =
    StationGraphViewModel("0b7e3f5a", stream, TimeZone.UTC, repo, compute = StandardTestDispatcher(testScheduler))

  @Test
  fun loads_the_streams_points_sorted_and_cut_to_24_h() = runTest {
    val repo = FakeStationMeasurementsRepository(Result.success(listOf(NEWEST, TOO_OLD, OLDEST_KEPT)))
    val vm = viewModel(repo)
    assertEquals(StationGraphState.Loading, vm.state.value)

    advanceUntilIdle()

    assertEquals(StationGraphState.Ready(listOf(OLDEST_KEPT, NEWEST)), vm.state.value)
    assertEquals(listOf(MeasurementsRequest("0b7e3f5a", "AirBeam-PM2.5", MEASURED_AT, TimeZone.UTC)), repo.requests)
  }

  @Test
  fun a_stream_that_never_reported_is_an_empty_graph_without_a_request() = runTest {
    val repo = FakeStationMeasurementsRepository(Result.success(listOf(NEWEST)))
    val vm = viewModel(repo, STREAM.copy(value = null, measuredAt = null))

    advanceUntilIdle()

    assertEquals(StationGraphState.Ready(emptyList()), vm.state.value)
    assertTrue(repo.requests.isEmpty())
  }

  @Test
  fun a_failed_load_is_Error() = runTest {
    val vm = viewModel(FakeStationMeasurementsRepository(Result.failure(RuntimeException("offline"))))

    advanceUntilIdle()

    assertEquals(StationGraphState.Error, vm.state.value)
  }
  @Test
  fun retrying_after_an_error_shows_the_graph() = runTest {
    val repo = FakeStationMeasurementsRepository(Result.failure(RuntimeException("offline")))
    val vm = viewModel(repo)
    advanceUntilIdle()

    repo.result = Result.success(listOf(NEWEST))
    vm.load(); advanceUntilIdle()

    assertEquals(StationGraphState.Ready(listOf(NEWEST)), vm.state.value)
  }
}

private val MEASURED_AT = Instant.fromEpochMilliseconds(1793282400000)
private val STREAM = StationStream(7, "AirBeam-PM2.5", "µg/m³", value = 12, min = 5, max = 20, measuredAt = MEASURED_AT)
private val NEWEST = ChartPoint(200_000.0, 12.0)
private val OLDEST_KEPT = ChartPoint(200_000.0 - 86_400, 8.0)
private val TOO_OLD = ChartPoint(200_000.0 - 86_401, 99.0)
