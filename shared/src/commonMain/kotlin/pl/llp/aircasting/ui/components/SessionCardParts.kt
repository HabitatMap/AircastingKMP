package pl.llp.aircasting.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Figma "UserTag": hairline-outlined pill. */
@Composable
fun UserTag(tag: String) {
  Text(
    tag,
    Modifier.border(0.65.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
      .padding(horizontal = 8.dp, vertical = 2.dp),
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

/** Section count next to a title: grey when 0, primary-tinted otherwise. */
@Composable
fun CountBadge(count: Int) {
  val empty = count == 0
  Text(
    count.toString(),
    Modifier.clip(CircleShape)
      .background(
        if (empty) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
      )
      .padding(horizontal = 8.dp, vertical = 2.dp),
    style = MaterialTheme.typography.labelLargeEmphasized,
    color = if (empty) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer,
  )
}

/** Figma "Button - outline" on session cards. */
@Composable
fun CardButton(
  label: String,
  onClick: () -> Unit,
  borderColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  icon: DrawableResource? = null,
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.2.dp, borderColor),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      contentColor = contentColor,
    ),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
  ) {
    icon?.let {
      Icon(painterResource(it), contentDescription = null, Modifier.size(16.dp))
      Spacer(Modifier.width(6.dp))
    }
    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
  }
}
