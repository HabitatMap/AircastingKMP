package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.llp.aircasting.explore.StationDetailsScreen
import pl.llp.aircasting.explore.StationDetailsViewModel
import pl.llp.aircasting.explore.StationGraph

/** Only the uuid: `/explore/stations/{uuid}` returns the rest (docs/API_V3.md → Explore station details). */
@Serializable
data class StationDetailsRoute(val uuid: String)

@Composable
fun StationDetailsDestination(uuid: String, onBack: () -> Unit) {
  val vm = koinViewModel<StationDetailsViewModel> { parametersOf(uuid) }
  val state by vm.state.collectAsStateWithLifecycle()
  StationDetailsScreen(
    state, onBack = onBack, onRetry = vm::load, onSelectStream = vm::selectStream,
    onThresholdChange = vm::saveThreshold,
    graph = { details, stream, threshold, modifier -> StationGraph(details, stream, threshold, modifier) },
  )
}