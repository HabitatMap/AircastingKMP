package pl.llp.aircasting.mydata

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.components.CountBadge

/**
 * My Data tab (Figma 925:18415, empty 925:19406). Only Past Sessions so far; Favorites, search
 * and the card menu come later.
 */
@Composable
fun MyDataScreen(
  state: PastSessionsUiState,
  onSelectStream: (uuid: String, sensorName: String) -> Unit,
  onViewAllPast: () -> Unit,
  onSeeDetails: (uuid: String, sensorName: String?) -> Unit,
  onSeeFixedDetails: (uuid: String, sensorName: String?) -> Unit,
) {
  val strings = LocalStrings.current
  LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 48.dp)) {
    item(key = "past-header") {
      SectionTitle(
        strings.myDataPastSessions,
        count = state.sessions.size,
        onViewAll = onViewAllPast.takeIf { state.sessions.isNotEmpty() },
      )
    }
    item(key = "past-body") {
      if (state.sessions.isEmpty()) {
        EmptyCard(strings.myDataNoSessionsTitle, strings.myDataNoSessionsBody, Modifier.padding(horizontal = 24.dp))
      } else {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          items(state.sessions, key = { it.uuid }) { card ->
            PastSessionCardView(
              card,
              summaryOnly = true,
              onSelectStream = { onSelectStream(card.uuid, it) },
              onSeeDetails = { (if (card.isFixed) onSeeFixedDetails else onSeeDetails)(card.uuid, card.selected?.sensorName) },
              modifier = Modifier.width(320.dp),
            )
          }
        }
      }
    }
  }
}

/** Title + count, and "View all ›" once there's something to view. */
@Composable
private fun SectionTitle(title: String, count: Int, onViewAll: (() -> Unit)?) {
  val colors = MaterialTheme.colorScheme
  Row(
    Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 24.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onSurfaceVariant)
      CountBadge(count)
    }
    onViewAll?.let {
      TextButton(onClick = it) {
        Text(LocalStrings.current.myDataViewAll, style = MaterialTheme.typography.labelLarge, color = colors.primary)
        Icon(
          painterResource(Res.drawable.ic_arrow_forward_ios),
          contentDescription = null,
          Modifier.padding(start = 8.dp).size(16.dp),
          tint = colors.primary,
        )
      }
    }
  }
}

@Composable
private fun EmptyCard(title: String, body: String, modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier.fillMaxWidth()
      .padding(top = 8.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(colors.surfaceContainerLowest)
      .padding(horizontal = 24.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
    Text(body, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
  }
}
