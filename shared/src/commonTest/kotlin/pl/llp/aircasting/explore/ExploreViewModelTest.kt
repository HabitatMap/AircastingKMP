package pl.llp.aircasting.explore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.GeoSquare
import pl.llp.aircasting.domain.MapCluster
import pl.llp.aircasting.domain.MapStation
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.home.FakeLocationProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val thresholds = FakeThresholdsRepository(
    listOf(
      SensorThreshold("AirBeam-PM2.5", 0, 9, 35, 55, 150),
      SensorThreshold("Government-PM2.5", 0, 12, 35, 55, 150),
    )
  )

  /** stateIn(WhileSubscribed) only runs while someone collects — the screen, here the test. */
  private fun TestScope.subscribe(vm: ExploreViewModel) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
  }

  @Test
  fun fetches_nothing_until_the_map_reports_a_viewport() = runTest {
    val repo = FakeExploreMapRepository()
    subscribe(ExploreViewModel(repo, thresholds, NO_LOCATION))

    advanceUntilIdle()

    assertEquals(emptyList(), repo.calls)
  }

  @Test
  fun fetches_once_for_the_last_viewport_after_the_camera_settles() = runTest {
    val repo = FakeExploreMapRepository()
    val vm = ExploreViewModel(repo, thresholds, NO_LOCATION)
    subscribe(vm)

    vm.onCameraIdle(NYC)
    advanceTimeBy(100) // still panning
    vm.onCameraIdle(NEWARK)
    advanceUntilIdle()

    assertEquals(listOf(NEWARK to ExploreFilters()), repo.calls)
  }

  @Test
  fun a_new_viewport_cancels_the_request_in_flight() = runTest {
    val repo = FakeExploreMapRepository { viewport, _ ->
      if (viewport == NYC) delay(5_000) // slow response
      emptyList()
    }
    val vm = ExploreViewModel(repo, thresholds, NO_LOCATION)
    subscribe(vm)

    vm.onCameraIdle(NYC)
    advanceTimeBy(1_000) // debounce passed, NYC request in flight
    vm.onCameraIdle(NEWARK)
    advanceUntilIdle()

    assertEquals(listOf(NEWARK), repo.completed)
  }

  @Test
  fun applying_filters_refetches_with_them() = runTest {
    val repo = FakeExploreMapRepository()
    val vm = ExploreViewModel(repo, thresholds, NO_LOCATION)
    subscribe(vm)
    vm.onCameraIdle(NYC)
    advanceUntilIdle()

    val govOzone = ExploreFilters(DataSource.Government, setOf(MeasurementType.Ozone, MeasurementType.NitrogenDioxide))
    vm.applyFilters(govOzone)
    advanceUntilIdle()

    assertEquals(listOf(NYC to ExploreFilters(), NYC to govOzone), repo.calls)
    assertEquals(govOzone, vm.state.value.filters)
  }

  @Test
  fun with_one_type_selected_each_station_is_coloured_by_its_own_sensor() = runTest {
    val repo = FakeExploreMapRepository { _, _ -> listOf(AIRBEAM_12, GOV_12) }
    val vm = ExploreViewModel(repo, thresholds, NO_LOCATION)
    subscribe(vm)

    vm.applyFilters(PM_ONLY)
    vm.onCameraIdle(NYC)
    advanceUntilIdle()

    // Same value 12, different sensors: AirBeam-PM2.5 low=9 → MEDIUM, Government-PM2.5 low=12 → LOW.
    assertEquals(
      listOf(
        ExploreMarker.Station(AIRBEAM_12, MeasurementLevel.MEDIUM),
        ExploreMarker.Station(GOV_12, MeasurementLevel.LOW),
      ),
      vm.state.value.markers,
    )
  }
  @Test
  fun a_clustered_response_draws_every_item_as_a_plain_dot() = runTest {
    // Values and a single selected type would normally colour the stations — clustering wins.
    val repo = FakeExploreMapRepository { _, _ -> listOf(AIRBEAM_12, GOV_12, CLUSTER) }
    val vm = ExploreViewModel(repo, thresholds, NO_LOCATION)
    subscribe(vm)

    vm.applyFilters(PM_ONLY)
    vm.onCameraIdle(NYC)
    advanceUntilIdle()

    assertEquals(
      listOf(
        ExploreMarker.Dot(AIRBEAM_12.location),
        ExploreMarker.Dot(GOV_12.location),
        ExploreMarker.Dot(CLUSTER.location),
      ),

      vm.state.value.markers,
    )
  }

  @Test
  fun with_all_or_several_types_selected_stations_are_uncoloured() = runTest {
    // Defensive: even if a value arrived, two selected types give no single threshold to apply.
    val repo = FakeExploreMapRepository { _, _ -> listOf(AIRBEAM_12) }
    val vm = ExploreViewModel(repo, thresholds, NO_LOCATION)
    subscribe(vm)
    vm.onCameraIdle(NYC)
    advanceUntilIdle()

    assertEquals(listOf(ExploreMarker.Station(AIRBEAM_12, level = null)), vm.state.value.markers)

    vm.applyFilters(ExploreFilters(measurementTypes = setOf(MeasurementType.ParticulateMatter, MeasurementType.Ozone)))
    advanceUntilIdle()

    assertEquals(listOf(ExploreMarker.Station(AIRBEAM_12, level = null)), vm.state.value.markers)
  }

  @Test
  fun a_threshold_edit_recolours_without_refetching() = runTest {
    val repo = FakeExploreMapRepository { _, _ -> listOf(AIRBEAM_12) }
    val vm = ExploreViewModel(repo, thresholds, NO_LOCATION)
    subscribe(vm)
    vm.applyFilters(PM_ONLY)
    vm.onCameraIdle(NYC)
    advanceUntilIdle()

    thresholds.save(SensorThreshold("AirBeam-PM2.5", 0, 20, 35, 55, 150))
    advanceUntilIdle()

    assertEquals(listOf(ExploreMarker.Station(AIRBEAM_12, MeasurementLevel.LOW)), vm.state.value.markers)
    assertEquals(1, repo.calls.size)
  }

  @Test
  fun a_failed_fetch_keeps_the_last_markers_and_flags_the_error() = runTest {
    var offline = false
    val repo = FakeExploreMapRepository { _, _ ->
      if (offline) error("offline")
      listOf(AIRBEAM_12)
    }
    val vm = ExploreViewModel(repo, thresholds, NO_LOCATION)
    subscribe(vm)
    vm.applyFilters(PM_ONLY)
    vm.onCameraIdle(NYC)
    advanceUntilIdle()

    offline = true
    vm.onCameraIdle(NEWARK)
    advanceUntilIdle()

    assertTrue(vm.state.value.error)
    assertEquals(listOf(ExploreMarker.Station(AIRBEAM_12, MeasurementLevel.MEDIUM)), vm.state.value.markers)
  }
  @Test
  fun starts_centred_on_the_user() = runTest {
    val vm = ExploreViewModel(FakeExploreMapRepository(), thresholds, FakeLocationProvider(HERE))
    subscribe(vm)

    advanceUntilIdle()

    assertEquals(HERE, vm.state.value.startCentre)
  }

  @Test
  fun without_a_location_the_map_keeps_its_default_start() = runTest {
    val vm = ExploreViewModel(FakeExploreMapRepository(), thresholds, NO_LOCATION)
    subscribe(vm)

    advanceUntilIdle()

    assertNull(vm.state.value.startCentre)
  }
  private companion object {
    val HERE = GeoLocation(40.78, -73.96)
    val NO_LOCATION = FakeLocationProvider(null)
    val NYC = MapViewport(GeoSquare(north = 40.95, south = 40.5, east = -73.7, west = -74.3), zoom = 10.0)
    val NEWARK = MapViewport(GeoSquare(north = 40.8, south = 40.7, east = -74.1, west = -74.2), zoom = 12.0)
    val PM_ONLY = ExploreFilters(measurementTypes = setOf(MeasurementType.ParticulateMatter))

    fun station(source: DataSource, value: Int?) = MapStation(
      uuid = "u-$source", source = source, title = "Station", provider = "provider",
      location = GeoLocation(40.7, -74.0), lastMeasuredAt = Instant.fromEpochMilliseconds(0),
      measurementTypes = listOf(MeasurementType.ParticulateMatter), value = value,
    )

    val AIRBEAM_12 = station(DataSource.AirBeam, 12)
    val GOV_12 = station(DataSource.Government, 12)
    val CLUSTER = MapCluster(GeoLocation(40.75, -73.98), count = 37)
  }
}