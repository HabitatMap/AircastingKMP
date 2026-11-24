package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.llp.aircasting.di.FixedSessionDetailsQualifier
import pl.llp.aircasting.explore.StationDetailsScreen
import pl.llp.aircasting.explore.StationDetailsViewModel
import pl.llp.aircasting.explore.StationGraph
import pl.llp.aircasting.map.StationMap

/**
 * One of our own fixed sessions, read from the backend (nothing fixed is stored locally). Same screen
 * as an Explore station (Figma 1138:28153: one dot on the map); [sensorName] = the Active card's chip.
 */
@Serializable
data class FixedSessionDetailsRoute(val uuid: String, val sensorName: String? = null)

@Composable
fun FixedSessionDetailsDestination(route: FixedSessionDetailsRoute, onBack: () -> Unit) {
  val vm = koinViewModel<StationDetailsViewModel>(qualifier = FixedSessionDetailsQualifier) {
    parametersOf(route.uuid, route.sensorName)
  }
  val state by vm.state.collectAsStateWithLifecycle()
  StationDetailsScreen(
    state, onBack = onBack, onRetry = vm::load, onSelectStream = vm::selectStream,
    onThresholdChange = vm::saveThreshold,
    graph = { details, stream, threshold, modifier ->
      StationGraph(details, stream, threshold, modifier, FixedSessionDetailsQualifier)
    },
    map = { details, stream, threshold, fullScreen, modifier ->
      StationMap(details, stream, threshold, interactive = fullScreen, modifier)
    },
  )
}
