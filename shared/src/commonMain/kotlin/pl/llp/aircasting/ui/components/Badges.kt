package pl.llp.aircasting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.aqStatus
import pl.llp.aircasting.ui.theme.LocalAqColors

/** Brand-tinted source tag, e.g. `GOV MONITOR`, `AIRBEAM FIXED` (Figma "Tag"). */
@Composable
fun TintedTag(text: String) {
  Box(
    Modifier.clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f))
      .padding(horizontal = 6.dp, vertical = 2.dp),
  ) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
  }
}

/** Air-quality level pill, e.g. `Moderate` (Figma "StatusBadge"). */
@Composable
fun LevelBadge(level: MeasurementLevel) {
  val c = LocalAqColors.current.forLevel(level)
  Box(
    Modifier.clip(RoundedCornerShape(100.dp))
      .background(c.copy(alpha = 0.12f))
      .padding(horizontal = 8.dp, vertical = 2.dp),
  ) {
    Text(level.aqStatus(LocalStrings.current).label, style = MaterialTheme.typography.labelSmall, color = c)
  }
}

/** Air-quality level dot, e.g. next to a value. */
@Composable
fun LevelDot(level: MeasurementLevel, size: Dp = 10.dp) {
  Box(Modifier.size(size).clip(CircleShape).background(LocalAqColors.current.forLevel(level)))
}
