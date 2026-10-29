package pl.llp.aircasting.settings

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

/** A rounded card, under an optional section [header]. Rows go in [content]. */
@Composable
fun SettingsSection(header: String?, content: @Composable ColumnScope.() -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
    header?.let {
      Text(
        it,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.outline,
      )
    }
    Column(
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .padding(horizontal = 16.dp),
      content = content,
    )
  }
}

/** Tappable row: optional [leading] icon, label, optional current [value], chevron. */
@Composable
fun SettingsLinkRow(
  title: String,
  subtitle: String? = null,
  value: String? = null,
  /** 1 truncates; the root list passes [Int.MAX_VALUE] so translated summaries wrap. */
  subtitleMaxLines: Int = 1,
  leading: (@Composable () -> Unit)? = null,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .heightIn(min = 48.dp)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    leading?.invoke()
    SettingsRowLabel(title, subtitle, Modifier.weight(1f), subtitleMaxLines)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      value?.let {
        Text(
          it,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        painterResource(Res.drawable.ic_arrow_forward_ios),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun SettingsRowLabel(title: String, subtitle: String?, modifier: Modifier = Modifier, subtitleMaxLines: Int = 1) {
  Column(modifier) {
    Text(
      title,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
    subtitle?.let {
      Text(
        it,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = subtitleMaxLines,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
