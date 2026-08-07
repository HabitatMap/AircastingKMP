package pl.llp.aircasting.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
  title: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  content: @Composable ColumnScope.() -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.background,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
  ) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
      SheetHeader(title, onCancel = onDismiss, onConfirm = onConfirm)
      content()
    }
  }
}

@Composable
private fun SheetHeader(title: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
  val strings = LocalStrings.current
  // Box, not a Row with weights: the title is centred on the sheet in the design, and "Cancel" is
  // wider than "Done", so weighted space would push it off-centre.
  Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 16.dp)) {
    TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
      Text(
        strings.cancel,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(
      title,
      modifier = Modifier.align(Alignment.Center),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
    )
    TextButton(onClick = onConfirm, modifier = Modifier.align(Alignment.CenterEnd)) {
      Text(
        strings.done,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}