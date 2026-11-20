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
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.explore.GraphContent
import pl.llp.aircasting.ui.chart.MIN_VISIBLE_SECONDS
import pl.llp.aircasting.ui.chart.MOBILE_MIN_VISIBLE_SECONDS

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
  // A mobile session reads every second, so it zooms in further than a fixed station's 1-min data.
  val minVisibleSeconds = if (session.type == SessionType.MOBILE) MOBILE_MIN_VISIBLE_SECONDS else MIN_VISIBLE_SECONDS
  GraphContent(state, onRetry = vm::load, stream, threshold, modifier, minVisibleSeconds)
}