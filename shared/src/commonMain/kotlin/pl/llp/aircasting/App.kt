package pl.llp.aircasting

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.navigation.AppNavHost
import pl.llp.aircasting.ui.theme.AircastingTheme

@Composable
@Preview
fun App(onRequestLocation: () -> Unit = {}) {
  val lyricist = rememberStrings(AppStrings)
  ProvideStrings(lyricist, LocalStrings) {
    AircastingTheme {
      AppNavHost(onRequestLocation)
    }
  }
}
