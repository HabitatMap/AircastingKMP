package pl.llp.aircasting.active

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.domain.TrackPoint
import pl.llp.aircasting.map.MapCamera
import kotlin.collections.emptyList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SessionMapViewModelTest {
  @BeforeTest
  fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
  @AfterTest
  fun tearDown() = Dispatchers.resetMain()

  private val first = TrackPoint(GeoLocation(52.0, 21.0), 3.0)
  private val second = TrackPoint(GeoLocation(52.1, 21.1), 6.0)

  @Test
  fun loads_then_follows_the_track_live() = runTest {
    val repo = FakeSessionDetailsRepository(track = MutableStateFlow(listOf(first)))
    val vm = SessionMapViewModel("s1", "AirBeamMini-PM2.5", repo)
    assertNull(vm.track.value) // loading: no map yet, rather than a map framed on nothing
    advanceUntilIdle()
    assertEquals(listOf(first), vm.track.value)

    repo.track.value = listOf(first, second); advanceUntilIdle()
    assertEquals(listOf(first, second), vm.track.value)
  }

  @Test
  fun a_failed_read_shows_an_empty_map() = runTest {
    val vm = SessionMapViewModel("s1", "AirBeamMini-PM2.5", FakeSessionDetailsRepository(failTrack = IllegalStateException()))

    advanceUntilIdle()

    assertEquals(emptyList(), vm.track.value)
  }

  @Test
  fun the_camera_follows_the_default_view_until_the_user_moves_it_and_centre_resumes_it() {
    val vm = SessionMapViewModel("s1", "AirBeamMini-PM2.5", FakeSessionDetailsRepository())
    assertNull(vm.userCamera.value) // null = default view, re-framed on every reading

    val moved = MapCamera(52.0, 21.0, 14f)
    vm.onUserMoved(moved)
    assertEquals(moved, vm.userCamera.value) // kept across graph/full-screen/screen-off, until...

    vm.recentre()
    assertNull(vm.userCamera.value) // ...Centre: back to the default view, following again
  }

  @Test
  fun centre_keeps_a_zoom_the_user_chose_but_not_one_they_only_panned_at() {
    val vm = SessionMapViewModel("s1", "AirBeamMini-PM2.5", FakeSessionDetailsRepository())
    vm.onUserMoved(MapCamera(52.0, 21.0, 14f)) // gesture start: the default view
    vm.onUserMoved(MapCamera(52.1, 21.1, 14f)) // a pan: same zoom
    vm.recentre()
    assertNull(vm.followZoom.value) // nothing chosen: keep auto-fitting the growing track

    vm.onUserMoved(MapCamera(52.0, 21.0, 14f))
    vm.onUserMoved(MapCamera(52.0, 21.0, 17f)) // a pinch
    vm.recentre()
    assertNull(vm.userCamera.value)
    assertEquals(17f, vm.followZoom.value) // Centre follows the user at their zoom
  }
}
