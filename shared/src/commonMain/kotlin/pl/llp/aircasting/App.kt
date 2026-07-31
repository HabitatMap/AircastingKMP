package pl.llp.aircasting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import pl.llp.aircasting.home.HomeViewModel
import pl.llp.aircasting.home.HomeScreen
import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.theme.AircastingTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(onRequestLocation: () -> Unit = {}) {
  val lyricist = rememberStrings(AppStrings) // resolves the system locale, falls back to "en"
  ProvideStrings(lyricist, LocalStrings) {
    AircastingTheme {
      val vm = koinViewModel<HomeViewModel>()
      val state by vm.state.collectAsStateWithLifecycle()
      LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
      HomeScreen(
        state = state,
        onRetry = vm::refresh,
        onRequestLocation = onRequestLocation,
      )
    }
  }
}