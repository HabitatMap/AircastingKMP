package pl.llp.aircasting.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.help.HelpViewModel
import pl.llp.aircasting.settings.help.SettingsHelpScreen

@Composable
fun HelpRoute(onBack: () -> Unit) {
  val vm = koinViewModel<HelpViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  val locale = LocalStrings.current.contentLocale
  val uriHandler = LocalUriHandler.current
  LaunchedEffect(locale) { vm.load(locale) }

  SettingsHelpScreen(
    state = state,
    onBack = onBack,
    onOpen = uriHandler::openUri,
    onRetry = { vm.load(locale) },
  )
}