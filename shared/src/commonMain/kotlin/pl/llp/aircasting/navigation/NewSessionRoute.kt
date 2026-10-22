package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.record.DevicePickerScreen
import pl.llp.aircasting.record.NewSessionStep
import pl.llp.aircasting.record.NewSessionViewModel
import pl.llp.aircasting.record.RecordMethodScreen
import pl.llp.aircasting.record.RecordingViewModel
import pl.llp.aircasting.record.SessionConfirmScreen
import pl.llp.aircasting.record.SessionDetailsScreen
import pl.llp.aircasting.record.StartState
import pl.llp.aircasting.scan.ScanUiState
import pl.llp.aircasting.scan.ScanViewModel
import pl.llp.aircasting.scan.rememberBluetoothPermission

@Serializable
data object NewSessionRoute

@Composable
fun NewSessionDestination(onExit: () -> Unit) {
  val vm = koinViewModel<NewSessionViewModel>()
  val recorder = koinViewModel<RecordingViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  val startState by recorder.startState.collectAsStateWithLifecycle()
  LaunchedEffect(startState) { if (startState == StartState.Started) onExit() }
  val back: () -> Unit = { if (state.step == NewSessionStep.Method) onExit() else vm.back() }

  when (state.step) {
    NewSessionStep.Method -> RecordMethodScreen(
      selected = state.form.method,
      onSelect = vm::select,
      onBack = back,
      onCancel = onExit,
      onNext = vm::next,
    )
    NewSessionStep.Details -> SessionDetailsScreen(
      state = state,
      onRename = vm::rename,
      onEditTags = vm::editTags,
      onAddTag = vm::addTag,
      onInterval = vm::setInterval,
      onBack = back,
      onCancel = onExit,
      onNext = vm::next,
    )
    NewSessionStep.Confirm -> SessionConfirmScreen(
      state = state,
      preparing = (startState as? StartState.Preparing)?.step,
      onBack = back,
      onCancel = onExit,
      onStart = { recorder.start(state.form) },
    )
    NewSessionStep.Device -> {
      val permission = rememberBluetoothPermission()
      val scanVm = koinViewModel<ScanViewModel>()
      // Subscribing is what starts the scan (WhileSubscribed), so we only subscribe once allowed.
      val devices = if (permission.granted) {
        val scan by scanVm.state.collectAsStateWithLifecycle()
        (scan as? ScanUiState.Scanning)?.devices.orEmpty()
      } else {
        emptyList()
      }
      DevicePickerScreen(
        devices = devices,
        selected = state.form.device,
        permissionGranted = permission.granted,
        onRequestPermission = permission::request,
        onSelect = vm::selectDevice,
        onBack = back,
        onCancel = onExit,
        onNext = vm::next,
      )
    }
  }
}