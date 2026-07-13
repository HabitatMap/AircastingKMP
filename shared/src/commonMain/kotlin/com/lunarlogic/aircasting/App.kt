package com.lunarlogic.aircasting

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.ui.scan.ScanUiState
import com.lunarlogic.aircasting.ui.scan.ScanViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App() {
  MaterialTheme {
    val vm = koinViewModel<ScanViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    when (val s = state) {
      is ScanUiState.Scanning -> LazyColumn { items(s.devices) { Text("${it.name} — ${it.device}") } }
      is ScanUiState.Error -> Text("Scan unavailable: ${s.reason}")
      ScanUiState.Idle -> Text("Starting…")
    }
  }
}