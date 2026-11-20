package pl.llp.aircasting.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import pl.llp.aircasting.domain.TrackPoint
import pl.llp.aircasting.map.MapCamera
import kotlin.math.abs

/**
 * One stream's track for our own mobile session, live from the DB. Null = still loading.
 * One instance per stream (keyed at the call site), like SessionGraphViewModel — so the inline
 * and full-screen maps share one subscription, and one camera.
 */
class SessionMapViewModel(
  uuid: String,
  sensorName: String,
  repository: SessionDetailsRepository,
) : ViewModel() {
  val track: StateFlow<List<TrackPoint>?> = repository.observeTrack(uuid, sensorName)
    // A local read failing is unexpected; an empty map beats an error state with nothing to retry.
    .catch { e -> log.w(e) { "Session $uuid $sensorName track failed" }; emit(emptyList()) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

  private val _userCamera = MutableStateFlow<MapCamera?>(null)

  /**
   * Where the user panned/zoomed to; null = the default view (TrackMap re-frames it on every
   * reading, so the map follows the user). Lives as long as the details screen's back-stack entry:
   * survives the graph tab, full screen, screen off and backgrounding; leaving details resets it.
   */
  val userCamera: StateFlow<MapCamera?> = _userCamera.asStateFlow()

  private val _followZoom = MutableStateFlow<Float?>(null)

  /**
   * A zoom the user picked by pinching; null = none, auto-fit the track. Centre keeps it: the
   * default view then centres on the user at this zoom instead of re-fitting. Reset with the rest
   * of the ViewModel when details are left.
   */
  val followZoom: StateFlow<Float?> = _followZoom.asStateFlow()

  /** Called at a gesture's start (with the view it starts from) and where it settles. */
  fun onUserMoved(camera: MapCamera) {
    val before = _userCamera.value
    // A pan leaves the zoom as it was; only a real change counts as the user choosing one.
    if (before != null && abs(camera.zoom - before.zoom) > ZoomEpsilon) _followZoom.value = camera.zoom
    _userCamera.value = camera
  }

  /** The Centre button: back to the default view, following again, at the user's zoom if any. */
  fun recentre() {
    _userCamera.value = null
  }

  private companion object {
    /** Map SDKs hand zoom back as Float; a pan can wobble it in the last digits. */
    const val ZoomEpsilon = 0.01f
    val log = Logger.withTag("SessionMap")
  }
}
