package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.settings.account.AccountAction
import pl.llp.aircasting.settings.account.AccountScreenState
import pl.llp.aircasting.settings.account.AccountViewModel
import pl.llp.aircasting.settings.account.ProfileField
import pl.llp.aircasting.settings.account.SettingsAccountScreen

@Composable
fun AccountRoute(onBack: () -> Unit) {
  val vm = koinViewModel<AccountViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

  LaunchedEffect(vm) {
    vm.sessionEnded.collect {
      Logger.withTag("Account").i { "session ended" }
      onBack()
    }
  }
  SettingsAccountScreen(
    state = state,
    onBack = onBack,
    onAction = { action ->
      when (action) {
        AccountAction.ChangePassword -> vm.startPasswordChange()
        AccountAction.ChangeEmail -> vm.startEdit(ProfileField.Email)
        AccountAction.ChangeUsername -> vm.startEdit(ProfileField.Username)
      }
    },
    onSignOut = vm::signOut,
    onDeleteAccount = vm::startAccountDeletion,
    onDeletionConfirmed = vm::sendDeletionCode,
    onDeletionCodeSubmit = vm::submitDeletionCode,
    onDeletionDismissed = vm::cancelAccountDeletion,
    onEditSave = vm::saveEdit,
    onEditDismissed = vm::cancelEdit,
    onPasswordSave = vm::changePassword,
    onPasswordDismissed = vm::cancelPasswordChange,
  )
}
