package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.auth.AuthScreen
import pl.llp.aircasting.auth.AuthViewModel
import pl.llp.aircasting.auth.ForgotPasswordScreen
import pl.llp.aircasting.auth.ForgotPasswordViewModel
import pl.llp.aircasting.auth.rememberAuthFormActions

@Serializable
data object AuthRoute

@Serializable
data object ForgotPasswordRoute

@Composable
fun AuthDestination(onForgotPassword: () -> Unit) {
  val vm = koinViewModel<AuthViewModel>()
  val form by vm.form.collectAsStateWithLifecycle()
  AuthScreen(form, rememberAuthFormActions(vm, onForgotPassword))
}

@Composable
fun ForgotPasswordDestination(onBack: () -> Unit) {
  val vm = koinViewModel<ForgotPasswordViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  ForgotPasswordScreen(
    state = state,
    onEmailChange = vm::onEmailChange,
    onSubmit = vm::submit,
    onBack = onBack,
  )
}

