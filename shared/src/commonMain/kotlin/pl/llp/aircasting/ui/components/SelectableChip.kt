package pl.llp.aircasting.ui.components

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_check
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

/**
 * Design-system option tile: 10dp corners, 1.5dp border, brand-accent tint when selected.
 * Padding is 12dp + the 1.5dp stroke, because Figma counts the stroke inside the padding.
 */
@Composable
fun SelectableChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  role: Role = Role.RadioButton,
  showCheckWhenSelected: Boolean = false,
) {
  val shape = RoundedCornerShape(10.dp)
  val colors = MaterialTheme.colorScheme
  val showCheck = selected && showCheckWhenSelected
  // Unselected chips reserve the check's 28dp (24 icon + 4 gap) as symmetric padding, so the
  // chip keeps its width when toggled and neighbours in a FlowRow don't reflow.
  val padding = when {
    !showCheckWhenSelected -> PaddingValues(horizontal = 13.5.dp)
    selected -> PaddingValues(start = 9.5.dp, end = 13.5.dp)
    else -> PaddingValues(horizontal = 25.5.dp)
  }
  Surface(
    modifier = modifier
      .height(44.dp)
      .clip(shape)
      .selectable(selected = selected, role = role, onClick = onClick),
    shape = shape,
    color = if (selected) colors.primaryContainer.copy(alpha = 0.08f) else colors.surfaceContainerLowest,
    contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
    border = BorderStroke(1.5.dp, if (selected) colors.primaryContainer else colors.outlineVariant),
  ) {
    Row(
      Modifier.fillMaxHeight().padding(padding),
      horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (showCheck) {
        Icon(painterResource(Res.drawable.ic_check), contentDescription = null, Modifier.size(24.dp))
      }
      Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
    }
  }
}
