package pl.llp.aircasting.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
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
  onSearchPlaces: (String) -> Unit,
  onSelectPlace: (PlaceSuggestion) -> Unit,
  onFlownTo: () -> Unit,
) {
  var query by rememberSaveable { mutableStateOf("") }
  val focus = LocalFocusManager.current
  Column(Modifier.fillMaxSize()) {
    ExploreTopBar(
      state.filters,
      query = query,
      onQueryChange = { query = it; onSearchPlaces(it) },
      onOpenFilters = onOpenFilters,
      onApplyFilters = onApplyFilters,
    )
    Box(Modifier.weight(1f).fillMaxWidth()) {
      ExploreMap(
        markers = state.markers,
        colourType = state.filters.colourType,
        startCentre = state.startCentre,
        flyTo = state.flyTo,
        onFlownTo = onFlownTo,
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
      if (state.suggestions.isNotEmpty()) {
        PlaceSuggestions(
          state.suggestions,
          onSelect = { place ->
            query = place.primary
            focus.clearFocus() // hides the keyboard so the map is visible
            onSelectPlace(place)
          },
          modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }
  }
  state.sheet?.let { sheet ->
    ExploreFilterSheet(sheet, onEdit = onEditFilters, onConfirm = onConfirmFilters, onDismiss = onDismissFilters)
  }
}

@Composable
private fun PlaceSuggestions(
  suggestions: List<PlaceSuggestion>,
  onSelect: (PlaceSuggestion) -> Unit,
  modifier: Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = colors.surfaceContainerLowest,
    shadowElevation = 3.dp,
  ) {
    Column {
      suggestions.forEach { place ->
        Column(
          Modifier.fillMaxWidth()
            .clickable { onSelect(place) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Text(place.primary, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
          if (place.secondary.isNotEmpty()) {
            Text(
              place.secondary,
              style = MaterialTheme.typography.bodyMedium,
              color = colors.onSurfaceVariant,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}