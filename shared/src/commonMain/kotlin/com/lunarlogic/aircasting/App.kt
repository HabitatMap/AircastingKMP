package com.lunarlogic.aircasting

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import co.touchlab.kermit.Logger
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
  MaterialTheme {
    val connector = koinInject<AirBeamConnector>()
    val devices by connector.scan().collectAsState(initial = emptyList())
    Logger.d(tag = "BLE") { "devices size: ${devices.size}" }
    devices.forEach { Logger.d(tag = "BLE") { it.id.toString() } }
    LazyColumn { items(devices) { Text("${it.name} — ${it.device}") } }
  }
}