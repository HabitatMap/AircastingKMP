package pl.llp.aircasting.record

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_location_on
import aircasting.shared.generated.resources.ic_my_location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.domain.GeoLocation
import pl.llp.aircasting.explore.ExploreMap
import pl.llp.aircasting.explore.PlaceSuggestion
import pl.llp.aircasting.explore.PlaceSuggestions
import pl.llp.aircasting.explore.SearchPill
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.wizard.WizardButton
import pl.llp.aircasting.ui.wizard.WizardScaffold

/** "Where's your AirBeam?": pan the map under a fixed pin; the pin is wherever the map settles. */
@Composable
fun AirBeamLocationScreen(
  state: NewSessionState,
  picker: PinPickerState,
  onSearch: (String) -> Unit,
  onSelectPlace: (PlaceSuggestion) -> Unit,
  onLocateMe: () -> Unit,
  onFlownTo: () -> Unit,
  onPinMoved: (GeoLocation) -> Unit,
  onBack: () -> Unit,
  onCancel: () -> Unit,
  onConfirm: () -> Unit,
) {
  val strings = LocalStrings.current
  var query by rememberSaveable { mutableStateOf("") }
  val focus = LocalFocusManager.current
  // The map opens on the pin once; after that the pin follows the map, never the other way round.
  val opensAt = remember { state.form.outdoorLocation }
  WizardScaffold(
    progress = state.step.progress,
    onBack = onBack,
    onCancel = onCancel,
    footer = { WizardButton(strings.confirmLocation, onConfirm, enabled = state.canContinue, showChevron = false) },
  ) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text(
        strings.locationTitle,
        style = MaterialTheme.typography.headlineMediumEmphasized,
        color = MaterialTheme.colorScheme.onBackground,
      )
      SearchPill(query, strings.locationSearchHint, onQueryChange = { query = it; onSearch(it) }, Modifier.fillMaxWidth())
      Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 16.dp)) {
        ExploreMap(
          markers = emptyList(),
          colourType = null,
          initialCamera = null, // opens on the pin via startCentre
          startCentre = opensAt,
          flyTo = picker.flyTo,
          onFlownTo = onFlownTo,
          onCameraIdle = { onPinMoved(it.centre) },
          onStationClick = {}, // no markers here
          modifier = Modifier.fillMaxSize(),
        )
        Icon(
          painterResource(Res.drawable.ic_location_on),
          contentDescription = null,
          // The glyph's tip is 2/24 above its bottom edge: lift it 20dp so the tip, not the middle, marks the spot.
          modifier = Modifier.align(Alignment.Center).offset(y = (-20).dp).size(48.dp),
          tint = MaterialTheme.colorScheme.primaryContainer,
        )
        SmallFloatingActionButton(
          onClick = onLocateMe,
          modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
          Icon(painterResource(Res.drawable.ic_my_location), contentDescription = strings.myLocation)
        }
        if (picker.suggestions.isNotEmpty()) {
          PlaceSuggestions(
            picker.suggestions,
            onSelect = { place ->
              query = place.primary
              focus.clearFocus() // hides the keyboard so the map is visible
              onSelectPlace(place)
            },
            modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
          )
        }
      }
    }
  }
}