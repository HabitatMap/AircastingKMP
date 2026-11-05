package pl.llp.aircasting.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.ui.chart.MeasurementChart
import pl.llp.aircasting.ui.components.ErrorState

/** Fixed, so the screen doesn't jump between loading, error and chart. Figma: 406 dp plot + hour labels. */
private val GraphHeight = 440.dp

/** The selected stream's 24 h graph, under the stats. [threshold] = the header's, so both colour alike. */
@Composable
fun StationGraph(
  station: StationDetails, stream: StationStream, threshold: SensorThreshold?, modifier: Modifier = Modifier,
) {
  // One ViewModel per stream id: switching chips keeps each stream's loaded points.
  val vm = koinViewModel<StationGraphViewModel>(key = "graph-${stream.id}") {
    parametersOf(station.uuid, stream, station.timeZone)
  }
  val state by vm.state.collectAsStateWithLifecycle()
  Box(modifier.fillMaxWidth().height(GraphHeight), contentAlignment = Alignment.Center) {
    when (val graph = state) {
      StationGraphState.Loading -> CircularProgressIndicator()
      StationGraphState.Error -> ErrorState(onRetry = vm::load)
      // key: a new stream starts from a fresh scroll/zoom, opened on its newest point.
      is StationGraphState.Ready -> if (graph.points.isNotEmpty()) key(stream.id) {
        // Height on the host itself: CartesianChartHost caps itself at 200 dp otherwise.
        MeasurementChart(graph.points, threshold, stream.unitSymbol, Modifier.fillMaxWidth().height(GraphHeight))
      }
    }
  }
}