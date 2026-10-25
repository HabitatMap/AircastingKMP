package pl.llp.aircasting.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
  Text(text, modifier, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
}
