package pl.llp.aircasting.mydata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.SettingsScaffold

/** Figma 925:21111, big cards. Search, filters and the list/card toggle come later. */
@Composable
fun PastSessionsScreen(
  state: PastSessionsUiState,
  onBack: () -> Unit,
  onSelectStream: (uuid: String, sensorName: String) -> Unit,
  onSeeDetails: (uuid: String, sensorName: String?) -> Unit,
) {
  SettingsScaffold(title = LocalStrings.current.pastSessionsTitle, onBack = onBack) { padding ->
    val direction = LocalLayoutDirection.current
    LazyColumn(
      Modifier.fillMaxSize(),
      contentPadding = PaddingValues(
        start = padding.calculateStartPadding(direction) + 24.dp,
        end = padding.calculateEndPadding(direction) + 24.dp,
        top = padding.calculateTopPadding() + 8.dp,
        bottom = padding.calculateBottomPadding() + 48.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      items(state.sessions, key = { it.uuid }) { card ->
        PastSessionCardView(
          card,
          summaryOnly = false,
          onSelectStream = { onSelectStream(card.uuid, it) },
          onSeeDetails = { onSeeDetails(card.uuid, card.selected?.sensorName) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}
