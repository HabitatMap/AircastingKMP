package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.llp.aircasting.active.SessionGraph
import pl.llp.aircasting.di.SessionDetailsQualifier
import pl.llp.aircasting.explore.StationDetailsScreen
import pl.llp.aircasting.explore.StationDetailsViewModel

/**
 * One of our own sessions, read live from the local DB. [sensorName] = the chip selected on the
 * Active card, so details open on the same stream; null when the card had no streams yet.
 */
@Serializable
data class SessionDetailsRoute(val uuid: String, val sensorName: String? = null)

@Composable
fun SessionDetailsDestination(route: SessionDetailsRoute, onBack: () -> Unit) {
  val vm = koinViewModel<StationDetailsViewModel>(qualifier = SessionDetailsQualifier) {
    parametersOf(route.uuid, route.sensorName)
  }
  val state by vm.state.collectAsStateWithLifecycle()
  StationDetailsScreen(
    state, onBack = onBack, onRetry = vm::load, onSelectStream = vm::selectStream,
    onThresholdChange = vm::saveThreshold,
    graph = { details, stream, threshold, modifier -> SessionGraph(details, stream, threshold, modifier) },
  )
}