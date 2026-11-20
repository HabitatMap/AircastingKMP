package pl.llp.aircasting.active

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.map.TrackMap

/**
 * The selected stream's whole-session track, growing with every reading. Interactive inline and
 * full screen; the camera lives in the per-stream ViewModel, so both share it.
 */
@Composable
fun SessionMap(
  session: StationDetails, stream: StationStream, threshold: SensorThreshold?,
  modifier: Modifier = Modifier,
) {
  val vm = koinViewModel<SessionMapViewModel>(key = "session-map-${stream.id}") {
    parametersOf(session.uuid, stream.sensorName)
  }
  val track by vm.track.collectAsStateWithLifecycle()
  val userCamera by vm.userCamera.collectAsStateWithLifecycle()
  val followZoom by vm.followZoom.collectAsStateWithLifecycle()
  track?.let {
    TrackMap(
      it, threshold, stream.unitSymbol, session.timeZone,
      // Details only open active sessions so far; a finished one will pass false (frame the track).
      live = true,
      userCamera, followZoom, onUserMoved = vm::onUserMoved, onRecentre = vm::recentre, modifier = modifier,
    )
  }
}
