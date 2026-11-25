package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.mydata.PastSessionsScreen
import pl.llp.aircasting.mydata.PastSessionsViewModel

/** My Data → Past Sessions → "View all". */
@Serializable
data object PastSessionsRoute

@Composable
fun PastSessionsDestination(
  onBack: () -> Unit,
  onOpenSession: (uuid: String, sensorName: String?) -> Unit,
  onOpenFixedSession: (uuid: String, sensorName: String?) -> Unit,
) {
  val vm = koinViewModel<PastSessionsViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  PastSessionsScreen(
    state, onBack = onBack, onSelectStream = vm::selectStream,
    onSeeDetails = onOpenSession, onSeeFixedDetails = onOpenFixedSession,
  )
}
