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
import pl.llp.aircasting.explore.GraphContent

/** The selected stream's whole-session graph, updating with every reading. */
@Composable
fun SessionGraph(
  session: StationDetails, stream: StationStream, threshold: SensorThreshold?, modifier: Modifier = Modifier,
) {
  // One ViewModel per stream id: switching chips keeps each stream's subscription.
  val vm = koinViewModel<SessionGraphViewModel>(key = "session-graph-${stream.id}") {
    parametersOf(session.uuid, stream.sensorName, session.timeZone)
  }
  val state by vm.state.collectAsStateWithLifecycle()
  GraphContent(state, onRetry = vm::load, stream, threshold, modifier)
}