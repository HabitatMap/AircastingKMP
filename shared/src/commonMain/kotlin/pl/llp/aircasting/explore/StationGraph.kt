package pl.llp.aircasting.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.ui.chart.MIN_VISIBLE_SECONDS
import pl.llp.aircasting.ui.chart.MeasurementChart
import pl.llp.aircasting.ui.components.ErrorState
import pl.llp.aircasting.ui.components.LoadingSpinner

/** The selected stream's 24 h graph, under the stats. [threshold] = the header's, so both colour alike. */
@Composable
fun StationGraph(
  station: StationDetails, stream: StationStream, threshold: SensorThreshold?, modifier: Modifier = Modifier,
  /** Which measurements source: null = Explore's public one; our own fixed session passes its qualifier. */
  source: Qualifier? = null,
) {
  // One ViewModel per stream id: switching chips keeps each stream's loaded points.
  val vm = koinViewModel<StationGraphViewModel>(qualifier = source, key = "graph-${stream.id}") {
    parametersOf(station.uuid, stream, station.timeZone)
  }
  val state by vm.state.collectAsStateWithLifecycle()
  GraphContent(state, onRetry = vm::load, stream, threshold, modifier)
}

/** Spinner, retry or chart for one stream: the same whichever ViewModel produced [state]. */
@Composable
internal fun GraphContent(
  state: StationGraphState,
  onRetry: () -> Unit,
  stream: StationStream,
  threshold: SensorThreshold?,
  modifier: Modifier = Modifier,
  minVisibleSeconds: Double = MIN_VISIBLE_SECONDS,
) {
  // Fills the caller's slot (the details screen sizes it), so loading, error and chart are the same size.
  Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when (state) {
      StationGraphState.Loading -> LoadingSpinner()
      StationGraphState.Error -> ErrorState(onRetry = onRetry)
      // key: a new stream starts from a fresh scroll/zoom, opened on its newest point.
      is StationGraphState.Ready -> if (state.points.isNotEmpty()) key(stream.id) {
        // Size on the host itself: CartesianChartHost caps itself at 200 dp otherwise.
        MeasurementChart(state.points, threshold, stream.unitSymbol, Modifier.fillMaxSize(), minVisibleSeconds)
      }
    }
  }
}