package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.settings.server.CustomDataServerScreen
import pl.llp.aircasting.settings.server.CustomDataServerViewModel
import pl.llp.aircasting.settings.server.CustomServerStep

@Composable
fun CustomDataServerRoute(onExit: () -> Unit) {
  val vm = koinViewModel<CustomDataServerViewModel>()
  val step by vm.step.collectAsStateWithLifecycle()

  LaunchedEffect(vm) { vm.finished.collect { onExit() } }

  CustomDataServerScreen(
    step = step,
    onBack = { if (step == CustomServerStep.Intro) onExit() else vm.back() },
    onExit = onExit,
    onEdit = vm::edit,
    onNext = vm::next,
    onUseOfficial = { vm.useOfficialServer() },
    onSave = { vm.save() },
    onCancelTest = vm::cancelTest,
  )
}