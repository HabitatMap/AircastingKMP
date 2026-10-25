package pl.llp.aircasting.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Figma "Button - outline": white fill, 1.2dp brand-accent border, 16dp corners. */
@Composable
fun OutlinedActionButton(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primaryContainer),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      contentColor = contentColor,
    ),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
  ) {
    Text(label, style = MaterialTheme.typography.titleMedium)
  }
}
