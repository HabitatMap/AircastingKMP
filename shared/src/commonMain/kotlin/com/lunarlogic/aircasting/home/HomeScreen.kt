package com.lunarlogic.aircasting.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lunarlogic.aircasting.home.components.AirQualityCard
import com.lunarlogic.aircasting.home.components.NearbyStationsSection

@Composable
fun HomeScreen(
  state: HomeScreenState,
  onRetry: () -> Unit,
  onRequestLocation: () -> Unit = {},
) {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when (state) {
      HomeScreenState.Loading -> CircularProgressIndicator()
      HomeScreenState.Error -> ErrorState(onRetry)
      is HomeScreenState.Content -> HomeContent(state.ui, onRequestLocation)
    }
  }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Something went wrong.", style = MaterialTheme.typography.titleMedium)
    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Try again") }
  }
}

@Composable
private fun HomeContent(ui: HomeUiState, onRequestLocation: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    Spacer(modifier = Modifier.height(50.dp)) // TODO: remove
    AirQualityCard(ui.airQuality, onRequestLocation)
    if (ui.nearby.isNotEmpty()) NearbyStationsSection(ui.nearby)
  }
}
