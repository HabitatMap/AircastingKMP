package pl.llp.aircasting.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.home.components.AirQualityCard
import pl.llp.aircasting.home.components.HomeTopBar
import pl.llp.aircasting.home.components.NearbyStationsSection
import pl.llp.aircasting.ui.components.ErrorState

@Composable
fun HomeScreen(
  state: HomeScreenState,
  onRetry: () -> Unit,
  onRequestLocation: () -> Unit = {},
  onOpenSettings: () -> Unit = {},
) {
  Column(Modifier.fillMaxSize()) {
    HomeTopBar(onOpenSettings)
    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
      when (state) {
        HomeScreenState.Loading -> CircularProgressIndicator()
        HomeScreenState.Error -> ErrorState(onRetry)
        is HomeScreenState.Content -> HomeContent(state.ui, onRequestLocation)
      }
    }
  }
}

@Composable
private fun HomeContent(ui: HomeUiState, onRequestLocation: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    AirQualityCard(ui.airQuality, onRequestLocation)
    if (ui.nearby.isNotEmpty()) NearbyStationsSection(ui.nearby)
  }
}
