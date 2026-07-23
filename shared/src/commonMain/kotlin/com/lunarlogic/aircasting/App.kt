package com.lunarlogic.aircasting

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunarlogic.aircasting.home.HomeViewModel
import com.lunarlogic.aircasting.home.HomeScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(onRequestLocation: () -> Unit = {}) {
  MaterialTheme {
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