package pl.llp.aircasting.mydata

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.active.chipLabel
import pl.llp.aircasting.active.valueText
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.components.CardButton
import pl.llp.aircasting.ui.components.LevelDot
import pl.llp.aircasting.ui.components.StreamChip
import pl.llp.aircasting.ui.components.TintedTag
import pl.llp.aircasting.ui.components.UserTag

/**
 * A finished session. [summaryOnly] = the My Data overview card (Figma 925:18415, "Session
 * average" only); otherwise the Past sessions list card (Figma 925:21111, Avg / Min / Max).
 */
@Composable
fun PastSessionCardView(
  card: PastSessionCard,
  summaryOnly: Boolean,
  onSelectStream: (sensorName: String) -> Unit,
  onSeeDetails: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalStrings.current
  val colors = MaterialTheme.colorScheme
  Column(
    modifier.clip(RoundedCornerShape(16.dp))
      .background(colors.surfaceContainerLowest)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      TintedTag(if (card.isPhoneMic) strings.activeTagPhoneMic else strings.activeTagAirBeamMobile)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(card.name, style = MaterialTheme.typography.titleSmallEmphasized, color = colors.onSurface, maxLines = 1)
          Text(card.timeRange, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        if (card.tags.isNotEmpty()) {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            card.tags.forEach { UserTag(it) }
          }
        }
      }
    }

    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      card.streams.forEach { stream ->
        StreamChip(
          chipLabel(stream.sensorName, strings),
          selected = stream.sensorName == card.selected?.sensorName,
          onClick = { onSelectStream(stream.sensorName) },
        )
      }
    }

    card.selected?.let { stream ->
      if (summaryOnly) {
        Stat(strings.myDataSessionAverage, stream.avg, stream.unitSymbol)
      } else {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Stat(strings.pastSessionsAvg, stream.avg, stream.unitSymbol, Modifier.weight(1f))
          Stat(strings.pastSessionsMin, stream.min, stream.unitSymbol, Modifier.weight(1f))
          Stat(strings.pastSessionsMax, stream.max, stream.unitSymbol, Modifier.weight(1f))
        }
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      HorizontalDivider(color = colors.surfaceContainerHighest)
      CardButton(strings.activeSeeDetails, onSeeDetails, colors.primary, colors.onSurface, Modifier.fillMaxWidth())
    }
  }
}

/** Label over `● 3 µg/m³`; a grey dot when the value has no level. */
@Composable
private fun Stat(label: String, value: SummaryValue, unit: String, modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  Column(modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = colors.outline)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      value.level?.let { LevelDot(it) }
        ?: Box(Modifier.size(10.dp).clip(CircleShape).background(colors.outlineVariant))
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          valueText(value.value),
          Modifier.alignByBaseline(),
          style = MaterialTheme.typography.headlineSmallEmphasized,
          color = colors.onBackground,
        )
        Text(unit, Modifier.alignByBaseline(), style = MaterialTheme.typography.labelMedium, color = colors.outline)
      }
    }
  }
}
