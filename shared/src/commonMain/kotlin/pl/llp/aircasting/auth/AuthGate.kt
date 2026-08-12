package pl.llp.aircasting.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.data.auth.AuthState

@Composable
fun AuthGate(
  onCancelled: () -> Unit,
  onForgotPassword: () -> Unit,
  content: @Composable () -> Unit,
) {
  val session by koinInject<AuthRepository>().state.collectAsStateWithLifecycle()
  when (session) {
    is AuthState.SignedIn -> content()
    AuthState.SignedOut -> AuthSheetHost(onDismiss = onCancelled, onForgotPassword = onForgotPassword)
  }
}

@Composable
fun AuthSheetHost(onDismiss: () -> Unit, onForgotPassword: () -> Unit) {
  val vm = koinViewModel<AuthViewModel>()
  val form by vm.form.collectAsStateWithLifecycle()
  AuthSheet(form, rememberAuthFormActions(vm, onForgotPassword), onDismiss)
}

@Composable
fun rememberAuthFormActions(vm: AuthViewModel, onForgotPassword: () -> Unit): AuthFormActions {
  val forgotPassword by rememberUpdatedState(onForgotPassword)
  return remember(vm) {
    AuthFormActions(
      onLoginChange = vm::onLoginChange,
      onEmailChange = vm::onEmailChange,
      onUsernameChange = vm::onUsernameChange,
      onPasswordChange = vm::onPasswordChange,
      onTogglePassword = vm::togglePasswordVisibility,
      onSwitchMode = vm::switchTo,
      onForgotPassword = { forgotPassword() },
      onSubmit = vm::submit,
    )
  }
}

