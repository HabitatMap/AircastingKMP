package pl.llp.aircasting.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings

/** Full-screen load failure: generic message + Retry. */
@Composable
fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
  val strings = LocalStrings.current
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(strings.errorTitle, style = MaterialTheme.typography.titleMedium)
    Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text(strings.errorRetry) }
  }
}
