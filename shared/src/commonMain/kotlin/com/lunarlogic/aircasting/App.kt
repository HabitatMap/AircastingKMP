package com.lunarlogic.aircasting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import com.lunarlogic.aircasting.home.HomeViewModel
import com.lunarlogic.aircasting.home.HomeScreen
import com.lunarlogic.aircasting.i18n.AppStrings
import com.lunarlogic.aircasting.i18n.LocalStrings
import com.lunarlogic.aircasting.chart.ChartSpikeScreen
import com.lunarlogic.aircasting.ui.theme.AircastingTheme
import org.koin.compose.viewmodel.koinViewModel

/** TODO(spike): flip back to false and delete once the Vico chart spike concludes. */
private const val CHART_SPIKE = true

@Composable
@Preview
fun App(onRequestLocation: () -> Unit = {}) {
  val lyricist = rememberStrings(AppStrings) // resolves the system locale, falls back to "en"
  ProvideStrings(lyricist, LocalStrings) {
    AircastingTheme {
      if (CHART_SPIKE) {
        ChartSpikeScreen()
        return@AircastingTheme
      }
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