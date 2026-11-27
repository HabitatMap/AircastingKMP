package pl.llp.aircasting.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Asks before a destructive action. The confirm button is error-colored, the dismiss one stays
 * neutral, so the dangerous choice is never the visually easy one.
 */
@Composable
fun ConfirmDialog(
  title: String,
  body: String,
  confirmLabel: String,
  dismissLabel: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    title = { Text(title) },
    text = { Text(body) },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
      ) { Text(confirmLabel) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
  )
}
