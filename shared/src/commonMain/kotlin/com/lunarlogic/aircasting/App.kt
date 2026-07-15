package com.lunarlogic.aircasting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunarlogic.aircasting.ui.scan.ScanUiState
import com.lunarlogic.aircasting.ui.scan.ScanViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App() {
  MaterialTheme {
    val vm = koinViewModel<ScanViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
      Spacer(modifier = Modifier.height(300.dp))
      when (val s = state) {
        is ScanUiState.Scanning -> LazyColumn { items(s.devices) { Text("${it.name} — ${it.device}") } }
        is ScanUiState.Error -> Text("Scan unavailable: ${s.reason}")
        ScanUiState.Idle -> Text("Starting…")
      }
    }
  }
}