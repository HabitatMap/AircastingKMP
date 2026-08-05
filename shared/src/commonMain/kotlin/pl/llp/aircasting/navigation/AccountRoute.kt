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
import pl.llp.aircasting.settings.account.SettingsAccountScreen

@Composable
fun AccountRoute(onBack: () -> Unit) {
  val vm = koinViewModel<AccountViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

  LaunchedEffect(vm) {
    vm.sessionEnded.collect {
      // TODO(login): navigate to the login screen and clear the whole back stack. Until that
      // screen exists, leaving Account is the most we can do — the app has no signed-out shell.
      Logger.withTag("Account").i { "session ended" }
      onBack()
    }
  }
  SettingsAccountScreen(
    state = state,
    onBack = onBack,
    onAction = { action ->
      when (action) {
        AccountAction.ResetPassword -> vm.resetPassword()
        AccountAction.ChangeEmail,
        AccountAction.ChangeUsername ->
          Logger.withTag("Account").w { "$action has no backend endpoint" }
      }
    },
    onSignOut = vm::signOut,
    onDeleteAccount = vm::startAccountDeletion,
    onDeletionConfirmed = vm::sendDeletionCode,
    onDeletionCodeSubmit = vm::submitDeletionCode,
    onDeletionDismissed = vm::cancelAccountDeletion,
  )
}
