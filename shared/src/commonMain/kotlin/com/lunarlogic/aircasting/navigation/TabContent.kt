package com.lunarlogic.aircasting.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunarlogic.aircasting.home.HomeScreen
import com.lunarlogic.aircasting.home.HomeViewModel
import com.lunarlogic.aircasting.i18n.LocalStrings
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TabContent(tab: AppTab, onRequestLocation: () -> Unit = {}) {
  when (tab) {
    AppTab.Home -> HomeRoute(onRequestLocation)
    else -> Placeholder(LocalStrings.current.label(tab))
  }
}

@Composable
private fun HomeRoute(onRequestLocation: () -> Unit) {
  val vm = koinViewModel<HomeViewModel>()
  val state by vm.state.collectAsStateWithLifecycle()
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
  HomeScreen(state = state, onRetry = vm::refresh, onRequestLocation = onRequestLocation)
}

@Composable
private fun Placeholder(title: String) {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(title, style = MaterialTheme.typography.titleLarge)
  }
}