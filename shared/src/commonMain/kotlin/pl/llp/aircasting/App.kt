package pl.llp.aircasting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import pl.llp.aircasting.i18n.AppStrings
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.navigation.AircastingNavBar
import pl.llp.aircasting.navigation.AppTab
import pl.llp.aircasting.navigation.TabContent
import pl.llp.aircasting.ui.theme.AircastingTheme

@Composable
@Preview
fun App(onRequestLocation: () -> Unit = {}) {
  val lyricist = rememberStrings(AppStrings)
  ProvideStrings(lyricist, LocalStrings) {
    AircastingTheme {
      var selected by rememberSaveable { mutableStateOf(AppTab.Home) }
      Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AircastingNavBar(selected, onSelect = { selected = it }) },
      ) { padding ->
        Box(Modifier.padding(padding)) {
          TabContent(selected, onRequestLocation)
        }
      }
    }
  }
}