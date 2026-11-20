package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.bluetooth.BluetoothRadio
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.record.AirBeamLocationScreen
import pl.llp.aircasting.record.DevicePickerScreen
import pl.llp.aircasting.record.DeviceStatusScreen
import pl.llp.aircasting.record.NewSessionStep
import pl.llp.aircasting.record.NewSessionViewModel
import pl.llp.aircasting.record.PinPickerViewModel
import pl.llp.aircasting.record.PowerOnScreen
import pl.llp.aircasting.record.RecordMethodScreen
import pl.llp.aircasting.record.RecordingMethod
import pl.llp.aircasting.record.RecordingViewModel
import pl.llp.aircasting.record.SessionConfirmScreen
import pl.llp.aircasting.record.SessionDetailsScreen
import pl.llp.aircasting.record.StartState
import pl.llp.aircasting.record.deviceStepUi
import pl.llp.aircasting.record.progress
import pl.llp.aircasting.record.rememberWifiNetworks
import pl.llp.aircasting.record.startFailure
import pl.llp.aircasting.scan.ScanViewModel
import pl.llp.aircasting.scan.rememberBluetoothPermission
import pl.llp.aircasting.settings.mic.rememberMicrophonePermission

@Serializable
data object NewSessionRoute

@Composable
fun NewSessionDestination(onExit: () -> Unit, onStarted: () -> Unit, onRequestLocation: () -> Unit) {
  // Legacy NewSessionController asks on wizard entry: mobile readings are stamped with the phone's
  // fix, and the pin picker starts there. The platform callback skips the prompt once decided;
  // PhoneLocation picks up a grant on its own.
  LaunchedEffect(Unit) { onRequestLocation() }
  val vm = koinViewModel<NewSessionViewModel>()
  val recorder = koinViewModel<RecordingViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  val startState by recorder.startState.collectAsStateWithLifecycle()
  LaunchedEffect(startState) { if (startState == StartState.Started) onStarted() }
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
      onEditTagDraft = vm::editTagDraft,
      onCommitTag = vm::commitTagDraft,
      onAddTag = vm::addTag,
      onRemoveTag = vm::removeTag,
      onIndoor = vm::setIndoor,
      onWifiName = vm::editWifiName,
      onWifiPassword = vm::editWifiPassword,
      wifiNetworks = if (state.form.method == RecordingMethod.AirBeamFixed) rememberWifiNetworks() else null,
      onInterval = vm::setInterval,
      onCellular = vm::setCellular,
      onBack = back,
      onCancel = onExit,
      onNext = vm::next,
    )
    NewSessionStep.Confirm -> {
      val micPermission = rememberMicrophonePermission()
      val needsMic = state.form.method == RecordingMethod.PhoneMicrophone
      // Start tapped before the grant: carry on by itself once the user allows it.
      var startOnGrant by remember { mutableStateOf(false) }
      LaunchedEffect(startOnGrant, micPermission.isGranted) {
        if (startOnGrant && micPermission.isGranted) {
          startOnGrant = false
          recorder.start(state.form)
        }
      }
      SessionConfirmScreen(
        state = state,
        preparing = (startState as? StartState.Preparing)?.step,
        failure = LocalStrings.current.startFailure(startState),
        onBack = { recorder.clearFailure(); back() },
        onCancel = onExit,
        onStart = {
          if (needsMic && !micPermission.isGranted) {
            startOnGrant = true
            micPermission.request()
          } else {
            recorder.start(state.form)
          }
        },
      )
    }
    NewSessionStep.Location -> {
      val picker = koinViewModel<PinPickerViewModel>()
      val pickerState by picker.state.collectAsStateWithLifecycle()
      AirBeamLocationScreen(
        state = state,
        picker = pickerState,
        onSearch = picker::search,
        onSelectPlace = picker::selectPlace,
        onLocateMe = picker::locateMe,
        onFlownTo = picker::onFlownTo,
        onPinMoved = vm::placePin,
        onBack = back,
        onCancel = onExit,
        onConfirm = vm::next,
      )
    }
    NewSessionStep.Bluetooth, NewSessionStep.PowerOn, NewSessionStep.Device -> {
      val strings = LocalStrings.current
      // Permission state is read synchronously, so this gate never flashes the screen behind it.
      // It also guards the Bluetooth step: Android's enable request needs BLUETOOTH_CONNECT.
      val permission = rememberBluetoothPermission()
      if (!permission.granted) {
        DeviceStatusScreen(
          title = strings.bluetoothPermissionTitle,
          body = strings.bluetoothPermissionBody,
          buttonLabel = strings.bluetoothPermissionAllow,
          showChevron = false,
          progress = state.step.progress,
          onButton = permission::request,
          onBack = back,
          onCancel = onExit,
        )
      } else when (state.step) {
        // The view model leaves this step by itself once the radio reports Bluetooth on.
        NewSessionStep.Bluetooth -> {
          val radio = koinInject<BluetoothRadio>()
          DeviceStatusScreen(
            title = strings.bluetoothOffTitle,
            body = strings.bluetoothOffBody,
            buttonLabel = strings.next,
            progress = NewSessionStep.Bluetooth.progress,
            onButton = radio::requestEnable,
            onBack = back,
            onCancel = onExit,
          )
        }
        NewSessionStep.PowerOn -> PowerOnScreen(onBack = back, onCancel = onExit, onNext = vm::next)
        else -> {
          // Subscribing starts the scan (WhileSubscribed), so it runs only while the picker shows.
          val scanVm = koinViewModel<ScanViewModel>()
          val scan by scanVm.state.collectAsStateWithLifecycle()
          DevicePickerScreen(
            state = deviceStepUi(scan),
            selected = state.form.device,
            onSelect = vm::selectDevice,
            onRescan = scanVm::rescan,
            onBack = back,
            onCancel = onExit,
            onNext = {
              state.form.device?.let(recorder::connect)
              vm.next()
            },
          )
        }
      }
    }
  }
}