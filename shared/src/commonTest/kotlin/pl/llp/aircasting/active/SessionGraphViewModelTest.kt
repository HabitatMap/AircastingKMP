package pl.llp.aircasting.active

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.explore.StationGraphState
import pl.llp.aircasting.ui.chart.ChartPoint
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SessionGraphViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val warsaw = TimeZone.of("Europe/Warsaw")

  private fun viewModel(repo: FakeSessionDetailsRepository) =
    SessionGraphViewModel("s1", "AirBeamMini-PM2.5", warsaw, repo)

  @Test
  fun shows_the_streams_points_from_the_whole_session() = runTest {
    val repo = FakeSessionDetailsRepository(points = MutableStateFlow(listOf(FIRST, TWO_DAYS_LATER)))
    val vm = viewModel(repo)
    assertEquals(StationGraphState.Loading, vm.state.value)

    advanceUntilIdle()

    // Unlike an Explore station, nothing is cut to the last 24 h.
    assertEquals(StationGraphState.Ready(listOf(FIRST, TWO_DAYS_LATER)), vm.state.value)
    assertEquals(listOf(PointsRequest("s1", "AirBeamMini-PM2.5", warsaw)), repo.pointRequests)
  }

  @Test
  fun no_readings_yet_is_an_empty_graph() = runTest {
    val vm = viewModel(FakeSessionDetailsRepository())

    advanceUntilIdle()

    assertEquals(StationGraphState.Ready(emptyList()), vm.state.value)
  }

  @Test
  fun a_new_reading_extends_the_graph_through_the_same_subscription() = runTest {
    val repo = FakeSessionDetailsRepository(points = MutableStateFlow(listOf(FIRST)))
    val vm = viewModel(repo)
    advanceUntilIdle()

    repo.points.value = listOf(FIRST, TWO_DAYS_LATER); advanceUntilIdle()

    assertEquals(StationGraphState.Ready(listOf(FIRST, TWO_DAYS_LATER)), vm.state.value)
    assertEquals(1, repo.pointRequests.size)
  }

  @Test
  fun a_failed_read_is_Error_and_retry_recovers() = runTest {
    val repo = FakeSessionDetailsRepository(points = MutableStateFlow(listOf(FIRST)), failPoints = RuntimeException("disk"))
    val vm = viewModel(repo)
    advanceUntilIdle()
    assertEquals(StationGraphState.Error, vm.state.value)

    repo.failPoints = null
    vm.load(); advanceUntilIdle()

    assertEquals(StationGraphState.Ready(listOf(FIRST)), vm.state.value)
  }
}

private val FIRST = ChartPoint(x = 0.0, y = 5.0)
private val TWO_DAYS_LATER = ChartPoint(x = 2 * 86_400.0, y = 7.0)
