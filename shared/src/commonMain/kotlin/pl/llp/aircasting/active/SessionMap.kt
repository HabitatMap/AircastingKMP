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

/** The selected stream's whole-session track, growing with every reading. */
@Composable
fun SessionMap(
  session: StationDetails, stream: StationStream, threshold: SensorThreshold?, interactive: Boolean,
  modifier: Modifier = Modifier,
) {
  val vm = koinViewModel<SessionMapViewModel>(key = "session-map-${stream.id}") {
    parametersOf(session.uuid, stream.sensorName)
  }
  val track by vm.track.collectAsStateWithLifecycle()
  track?.let { TrackMap(it, threshold, stream.unitSymbol, session.timeZone, interactive, modifier) }
}