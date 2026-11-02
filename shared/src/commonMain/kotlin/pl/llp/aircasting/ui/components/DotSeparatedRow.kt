package pl.llp.aircasting.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/** `a • b • c`: separate texts 8dp apart (Home station card subtitle, station details meta row). */
@Composable
fun DotSeparatedRow(parts: List<String>, style: TextStyle, color: Color) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    parts.forEachIndexed { i, part ->
      if (i > 0) Text("•", style = style, color = color)
      Text(part, style = style, color = color)
    }
  }
}
