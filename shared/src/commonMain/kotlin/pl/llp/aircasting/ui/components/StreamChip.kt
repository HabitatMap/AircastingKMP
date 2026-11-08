package pl.llp.aircasting.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Figma stream "Tag": 28dp pill, 1dp border, brand-accent border + tint when selected.
 * Not SelectableChip — that's the 44dp, 10dp-corner option tile.
 */
@Composable
fun StreamChip(label: String, selected: Boolean, onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier = Modifier.clip(CircleShape).selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    shape = CircleShape,
    color = if (selected) colors.primaryContainer.copy(alpha = 0.08f) else Color.Transparent,
    contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
    border = BorderStroke(1.dp, if (selected) colors.primaryContainer else colors.outlineVariant),
  ) {
    Text(label, Modifier.padding(start = 12.dp, end = 10.dp, top = 6.dp, bottom = 6.dp), style = MaterialTheme.typography.labelSmall)
  }
}
