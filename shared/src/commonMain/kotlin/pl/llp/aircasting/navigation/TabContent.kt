package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.llp.aircasting.home.HomeScreen
import pl.llp.aircasting.home.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.active.ActiveScreen
import pl.llp.aircasting.active.ActiveViewModel
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MeasurementType
import pl.llp.aircasting.explore.ExploreScreen
import pl.llp.aircasting.explore.ExploreViewModel
import pl.llp.aircasting.mydata.MyDataScreen
import pl.llp.aircasting.mydata.PastSessionsViewModel
import pl.llp.aircasting.record.RecordStartScreen

@Composable
fun TabContent(
  tab: AppTab,
  onRequestLocation: () -> Unit = {},
  onOpenSettings: () -> Unit = {},
  onStartNewSession: () -> Unit = {},
  onOpenStation: (uuid: String) -> Unit = {},
  onOpenSession: (uuid: String, sensorName: String?) -> Unit = { _, _ -> },
  onOpenPastSessions: () -> Unit = {},
  onOpenFixedSession: (uuid: String, sensorName: String?) -> Unit = { _, _ -> },
) {
  when (tab) {
    AppTab.Home -> HomeRoute(onRequestLocation, onOpenSettings)
    AppTab.Record -> RecordStartScreen(
      onStartNewSession = onStartNewSession,
      onRecoverData = { /* TODO: recover & sync flow */ },
    )
    AppTab.Explore -> ExploreRoute(onOpenStation)
    AppTab.Active -> ActiveRoute(onStartNewSession, onOpenSession, onOpenFixedSession)
    AppTab.MyData -> MyDataRoute(onOpenPastSessions, onOpenSession, onOpenFixedSession)
  }
}

@Composable
private fun MyDataRoute(
  onViewAllPast: () -> Unit,
  onOpenSession: (String, String?) -> Unit,
  onOpenFixedSession: (String, String?) -> Unit,
) {
  val vm = koinViewModel<PastSessionsViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  MyDataScreen(
    state,
    onSelectStream = vm::selectStream,
    onViewAllPast = onViewAllPast,
    onSeeDetails = onOpenSession,
    onSeeFixedDetails = onOpenFixedSession,
  )
}

@Composable
private fun ActiveRoute(
  onStartNewSession: () -> Unit,
  onOpenSession: (String, String?) -> Unit,
  onOpenFixedSession: (String, String?) -> Unit,
) {
  val vm = koinViewModel<ActiveViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  ActiveScreen(
    state = state,
    onSelectStream = vm::selectStream,
    onFinish = vm::requestFinish,
    onConfirmFinish = vm::confirmFinish,
    onDismissFinish = vm::dismissFinish,
    onStartMobileSession = onStartNewSession,
    onStartFixedSession = onStartNewSession,
    onStartMicSession = onStartNewSession,
    onSeeDetails = onOpenSession,
    onSeeFixedDetails = onOpenFixedSession,
    onReconnect = vm::reconnect,
  )
}

@Composable
private fun HomeRoute(onRequestLocation: () -> Unit, onOpenSettings: () -> Unit) {
  val vm = koinViewModel<HomeViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
  HomeScreen(
    state = state,
    onRetry = vm::refresh,
    onRequestLocation = onRequestLocation,
    onOpenSettings = onOpenSettings,
  )
}

@Composable
private fun ExploreRoute(onOpenStation: (String) -> Unit) {
  val vm = koinViewModel<ExploreViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  ExploreScreen(
    state = state,
    onCameraIdle = vm::onCameraIdle,
    onOpenFilters = vm::openFilters,
    onEditFilters = vm::editFilters,
    onConfirmFilters = vm::confirmFilters,
    onDismissFilters = vm::dismissFilters,
    onApplyFilters = vm::applyFilters,
    onSearchPlaces = vm::searchPlaces,
    onSelectPlace = vm::selectPlace,
    onFlownTo = vm::onFlownTo,
    onOpenStation = onOpenStation,
  )
}
