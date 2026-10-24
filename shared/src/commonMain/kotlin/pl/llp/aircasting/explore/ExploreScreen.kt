package pl.llp.aircasting.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.domain.ExploreFilters
import pl.llp.aircasting.domain.MapViewport
import pl.llp.aircasting.i18n.LocalStrings

@Composable
fun ExploreScreen(
  state: ExploreUiState,
  onCameraIdle: (MapViewport) -> Unit,
  onOpenFilters: () -> Unit,
  onEditFilters: (ExploreFilters) -> Unit,
  onConfirmFilters: () -> Unit,
  onDismissFilters: () -> Unit,
  onApplyFilters: (ExploreFilters) -> Unit,
) {
  Column(Modifier.fillMaxSize()) {
    ExploreTopBar(state.filters, onOpenFilters = onOpenFilters, onApplyFilters = onApplyFilters)
    Box(Modifier.weight(1f).fillMaxWidth()) {
      ExploreMap(
        markers = state.markers,
        colourType = state.filters.colourType,
        startCentre = state.startCentre,
        onCameraIdle = onCameraIdle,
        modifier = Modifier.fillMaxSize(),
      )
      if (state.error) {
        Surface(
          modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          shape = MaterialTheme.shapes.small,
        ) {
          Text(
            LocalStrings.current.errorTitle,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
          )
        }
      }
    }
  }
  state.sheet?.let { sheet ->
    ExploreFilterSheet(sheet, onEdit = onEditFilters, onConfirm = onConfirmFilters, onDismiss = onDismissFilters)
  }
}