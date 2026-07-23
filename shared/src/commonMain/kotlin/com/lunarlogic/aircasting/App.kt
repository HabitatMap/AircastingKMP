package com.lunarlogic.aircasting

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunarlogic.aircasting.home.HomeViewModel
import com.lunarlogic.aircasting.home.HomeScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(
  onRequestLocation: () -> Unit = {},
  // Bumped by the host after a location-permission result; re-runs the load with the new grant.
  refreshSignal: Int = 0,
) {
  MaterialTheme {
    val vm = koinViewModel<HomeViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(refreshSignal) {
      if (refreshSignal > 0) vm.refresh()
    }
    HomeScreen(
      state = state,
      onRetry = vm::refresh,
      onRequestLocation = onRequestLocation,
    )
  }
}