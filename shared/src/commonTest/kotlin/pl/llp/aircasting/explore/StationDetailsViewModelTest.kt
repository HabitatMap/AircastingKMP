package pl.llp.aircasting.explore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.session.SessionType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StationDetailsViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  /** stateIn(WhileSubscribed) only runs while someone collects — the screen, here the test. */
  private fun TestScope.subscribe(vm: StationDetailsViewModel) = vm.also {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
  }

  private fun content(vm: StationDetailsViewModel) = vm.state.value as StationDetailsState.Content

  @Test
  fun loads_the_station_by_uuid_with_the_first_stream_selected() = runTest {
    val repo = FakeStationDetailsRepository(Result.success(STATION))
    val vm = subscribe(StationDetailsViewModel(repo.observe("u1"), FakeThresholdsRepository(emptyList())))
    assertEquals(StationDetailsState.Loading, vm.state.value)

    advanceUntilIdle()

    assertEquals(StationDetailsState.Content(STATION, selectedStreamId = 1), vm.state.value)
    assertEquals(listOf("u1"), repo.uuids)
  }

  @Test
  fun a_failed_load_is_Error() = runTest {
    val repo = FakeStationDetailsRepository(Result.failure(RuntimeException("offline")))
    val vm = subscribe(StationDetailsViewModel(repo.observe("u1"), FakeThresholdsRepository(emptyList())))

    advanceUntilIdle()

    assertEquals(StationDetailsState.Error, vm.state.value)
  }

  @Test
  fun retrying_after_an_error_shows_the_station() = runTest {
    val repo = FakeStationDetailsRepository(Result.failure(RuntimeException("offline")))
    val vm = subscribe(StationDetailsViewModel(repo.observe("u1"), FakeThresholdsRepository(emptyList())))
    advanceUntilIdle()

    repo.result = Result.success(STATION)
    vm.load(); advanceUntilIdle()

    assertEquals(StationDetailsState.Content(STATION, selectedStreamId = 1), vm.state.value)
  }

  @Test
  fun selecting_a_chip_switches_the_shown_stream() = runTest {
    val repo = FakeStationDetailsRepository(Result.success(STATION))
    val vm = subscribe(StationDetailsViewModel(repo.observe("u1"), FakeThresholdsRepository(emptyList())))
    advanceUntilIdle()

    vm.selectStream(2); advanceUntilIdle()

    assertEquals(PM25, content(vm).selected)
  }

  @Test
  fun the_selected_streams_local_threshold_levels_its_values() = runTest {
    val repo = FakeStationDetailsRepository(Result.success(STATION))
    val vm = subscribe(StationDetailsViewModel(repo.observe("u1"), FakeThresholdsRepository(listOf(PM1_T, PM25_T))))

    advanceUntilIdle()

    assertEquals(PM1_T, content(vm).threshold)
    assertEquals(MeasurementLevel.LOW, content(vm).levelOf(8)) // 0 ≤ 8 ≤ 9
  }

  @Test
  fun switching_stream_switches_the_threshold() = runTest {
    val repo = FakeStationDetailsRepository(Result.success(STATION))
    val vm = subscribe(StationDetailsViewModel(repo.observe("u1"), FakeThresholdsRepository(listOf(PM1_T, PM25_T))))
    advanceUntilIdle()

    vm.selectStream(2); advanceUntilIdle()

    assertEquals(PM25_T, content(vm).threshold)
  }

  @Test
  fun a_sensor_without_a_local_threshold_stays_uncoloured() = runTest {
    val repo = FakeStationDetailsRepository(Result.success(STATION))
    val vm = subscribe(StationDetailsViewModel(repo.observe("u1"), FakeThresholdsRepository(listOf(PM25_T))))

    advanceUntilIdle()

    assertNull(content(vm).threshold)
    assertNull(content(vm).levelOf(8))
  }

  @Test
  fun editing_a_threshold_recolours_without_refetching() = runTest {
    val repo = FakeStationDetailsRepository(Result.success(STATION))
    val thresholds = FakeThresholdsRepository(listOf(PM1_T))
    val vm = subscribe(StationDetailsViewModel(repo.observe("u1"), thresholds))
    advanceUntilIdle()

    thresholds.save(PM1_T.copy(low = 5)); advanceUntilIdle()

    assertEquals(MeasurementLevel.MEDIUM, content(vm).levelOf(8)) // now 8 > low
    assertEquals(listOf("u1"), repo.uuids)
  }

  @Test
  fun a_model_spelled_stream_uses_its_parameters_threshold() = runTest {
    val station = STATION.copy(streams = listOf(PM1.copy(sensorName = "AirBeamMini-PM1")))
    val repo = FakeStationDetailsRepository(Result.success(station))
    val thresholds = FakeThresholdsRepository(listOf(PM1_T), aliases = mapOf("AirBeamMini-PM1" to "AirBeam-PM1"))
    val vm = subscribe(StationDetailsViewModel(repo.observe("u1"), thresholds))

    advanceUntilIdle()

    assertEquals(PM1_T, content(vm).threshold)
  }

  @Test
  fun a_slider_edit_saves_one_override_for_every_model_spelling() = runTest {
    val station = STATION.copy(streams = listOf(PM1.copy(sensorName = "AirBeamMini-PM1")))
    val repo = FakeStationDetailsRepository(Result.success(station))
    val thresholds = FakeThresholdsRepository(listOf(PM1_T), aliases = mapOf("AirBeamMini-PM1" to "AirBeam-PM1"))
    val vm = subscribe(StationDetailsViewModel(repo.observe("u1"), thresholds))
    advanceUntilIdle()

    vm.saveThreshold(content(vm).threshold!!.copy(low = 5)); advanceUntilIdle()

    assertEquals(5, content(vm).threshold?.low)
    assertEquals(5, thresholds.observe().first().forSensor("AirBeam-PM1")?.low)
  }

  @Test
  fun the_selected_streams_default_is_exposed_alongside_the_users_edit() = runTest {
    val thresholds = FakeThresholdsRepository(listOf(PM1_T))
    val vm = subscribe(StationDetailsViewModel(FakeStationDetailsRepository(Result.success(STATION)).observe("u1"), thresholds))
    advanceUntilIdle()

    vm.saveThreshold(PM1_T.copy(low = 5)); advanceUntilIdle()

    assertEquals(5, content(vm).threshold?.low)
    assertEquals(PM1_T, content(vm).defaultThreshold)
  }

  @Test
  fun saving_the_default_values_drops_the_override() = runTest {
    val thresholds = FakeThresholdsRepository(listOf(PM1_T))
    val vm = subscribe(StationDetailsViewModel(FakeStationDetailsRepository(Result.success(STATION)).observe("u1"), thresholds))
    advanceUntilIdle()
    vm.saveThreshold(PM1_T.copy(low = 5)); advanceUntilIdle()

    vm.saveThreshold(PM1_T); advanceUntilIdle()

    assertEquals(PM1_T, content(vm).threshold)
    assertEquals(emptyList(), thresholds.overrides.value)
  }

  @Test
  fun opens_on_the_preferred_stream() = runTest {
    val vm = subscribe(
      StationDetailsViewModel(flowOf(STATION), FakeThresholdsRepository(emptyList()), preferredSensor = "AirBeam-PM2.5"),
    )

    advanceUntilIdle()

    assertEquals(2, content(vm).selectedStreamId)
  }

  @Test
  fun an_unknown_preferred_sensor_falls_back_to_the_first_stream() = runTest {
    val vm = subscribe(
      StationDetailsViewModel(flowOf(STATION), FakeThresholdsRepository(emptyList()), preferredSensor = "AirBeam-NO2"),
    )

    advanceUntilIdle()

    assertEquals(1, content(vm).selectedStreamId)
  }

  @Test
  fun a_live_update_refreshes_the_values_and_keeps_the_chosen_stream() = runTest {
    val live = MutableStateFlow<StationDetails?>(STATION)
    val vm = subscribe(StationDetailsViewModel(live, FakeThresholdsRepository(emptyList())))
    advanceUntilIdle()
    vm.selectStream(2); advanceUntilIdle()

    live.value = STATION.copy(streams = listOf(PM1, PM25.copy(value = 20))); advanceUntilIdle()

    assertEquals(2, content(vm).selectedStreamId)
    assertEquals(20, content(vm).selected?.value)
  }

  @Test
  fun a_session_that_no_longer_exists_is_Error() = runTest {
    val vm = subscribe(StationDetailsViewModel(flowOf(null), FakeThresholdsRepository(emptyList())))

    advanceUntilIdle()

    assertEquals(StationDetailsState.Error, vm.state.value)
  }
}

// Two streams of one measurement type: why selection is by stream id, not MeasurementType.
private val PM1 = StationStream(
  id = 1, sensorName = "AirBeam-PM1", unitSymbol = "µg/m³", value = 8, min = 3, max = 10,
  measuredAt = Instant.fromEpochMilliseconds(1793282400000),
)
private val PM25 = PM1.copy(id = 2, sensorName = "AirBeam-PM2.5", value = 12)
private val STATION = StationDetails(
  uuid = "0b7e3f5a", type = SessionType.FIXED, source = DataSource.AirBeam, title = "Garden", provider = "jane", deviceModel = "AirBeamMini",
  timeZone = TimeZone.UTC, streams = listOf(PM1, PM25),
)
private val PM1_T = SensorThreshold("AirBeam-PM1", 0, 9, 35, 55, 150)
private val PM25_T = SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150)
