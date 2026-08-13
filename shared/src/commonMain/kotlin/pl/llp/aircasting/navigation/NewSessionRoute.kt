package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.record.NewSessionStep
import pl.llp.aircasting.record.NewSessionViewModel
import pl.llp.aircasting.record.RecordMethodScreen
import pl.llp.aircasting.record.SessionConfirmScreen
import pl.llp.aircasting.record.SessionDetailsScreen

@Serializable
data object NewSessionRoute

@Composable
fun NewSessionDestination(onExit: () -> Unit) {
  val vm = koinViewModel<NewSessionViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
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
      onBack = back,
      onCancel = onExit,
      onStart = { /* TODO: create the Session and start recording */ },
    )
  }
}