package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.bluetooth.BluetoothRadio
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.record.DevicePickerScreen
import pl.llp.aircasting.record.DeviceStatusScreen
import pl.llp.aircasting.record.DeviceStepUi
import pl.llp.aircasting.record.NewSessionStep
import pl.llp.aircasting.record.NewSessionViewModel
import pl.llp.aircasting.record.RecordMethodScreen
import pl.llp.aircasting.record.RecordingViewModel
import pl.llp.aircasting.record.SessionConfirmScreen
import pl.llp.aircasting.record.SessionDetailsScreen
import pl.llp.aircasting.record.StartState
import pl.llp.aircasting.record.deviceStepUi
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
      val strings = LocalStrings.current
      val permission = rememberBluetoothPermission()
      val radio = koinInject<BluetoothRadio>()
      val scanVm = koinViewModel<ScanViewModel>()

      // Radio state only after permission: on iOS, creating a CBCentralManager is what prompts.
      val bluetoothOff by remember(radio, permission.granted) {
        if (permission.granted) radio.isOff else flowOf(false)
      }.collectAsStateWithLifecycle(initialValue = false)

      // Subscribing starts the scan (WhileSubscribed), so only subscribe when it can succeed.
      val scan = if (permission.granted && !bluetoothOff) {
        scanVm.state.collectAsStateWithLifecycle().value
      } else {
        ScanUiState.Idle
      }

      when (val ui = deviceStepUi(permission.granted, bluetoothOff, scan)) {
        DeviceStepUi.PermissionNeeded -> DeviceStatusScreen(
          title = strings.bluetoothPermissionTitle,
          body = strings.bluetoothPermissionBody,
          buttonLabel = strings.bluetoothPermissionAllow,
          showChevron = false,
          onButton = permission::request,
          onBack = back,
          onCancel = onExit,
        )
        DeviceStepUi.BluetoothOff -> DeviceStatusScreen(
          title = strings.bluetoothOffTitle,
          body = strings.bluetoothOffBody,
          buttonLabel = strings.next,
          onButton = radio::requestEnable,
          onBack = back,
          onCancel = onExit,
        )
        is DeviceStepUi.Devices -> DevicePickerScreen(
          state = ui,
          selected = state.form.device,
          onSelect = vm::selectDevice,
          onRescan = scanVm::rescan,
          onBack = back,
          onCancel = onExit,
          onNext = vm::next,
        )
      }
    }
  }
}