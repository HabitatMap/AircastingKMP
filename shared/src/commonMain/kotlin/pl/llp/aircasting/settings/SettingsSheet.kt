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
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.llp.aircasting.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
  title: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  confirmEnabled: Boolean = true,
  confirmLabel: String = LocalStrings.current.done,
  // Set true to slide the sheet out and then call onDismiss, e.g. after a Save that passed validation.
  closing: Boolean = false,
  content: @Composable ColumnScope.() -> Unit,
) {
  // Open straight to full content height; the half-height stop hid the footer buttons.
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  LaunchedEffect(closing) { if (closing) { sheetState.hide(); onDismiss() } }
  val motion = MaterialTheme.motionScheme
  MaterialTheme(motionScheme = remember(motion) { SheetHideMotion(motion) }) {
    ModalBottomSheet(
      // Swipe and scrim taps already animate out before calling this.
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.background,
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
      dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
    ) {
      // The app's own motion back for the content: only the sheet's hide is retuned.
      MaterialTheme(motionScheme = motion) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
          SheetHeader(
            title,
            // Removing a ModalBottomSheet from composition skips its exit animation: slide out first.
            onCancel = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
            onConfirm = onConfirm,
            confirmEnabled = confirmEnabled,
            confirmLabel = confirmLabel,
          )
          content()
        }
      }
    }
  }
}

/**
 * Material3 1.5.0-alpha (BottomSheet.kt) hides a sheet with `fastEffectsSpec`: a critically damped
 * spring of stiffness 3800, ~100 ms, which reads as no animation at all. This hides it with the
 * same spatial spring it opens with. Drop it once Material ships a proper hide motion.
 */
private class SheetHideMotion(base: MotionScheme) : MotionScheme by base {
  override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = defaultSpatialSpec()
}

@Composable
private fun SheetHeader(
  title: String,
  onCancel: () -> Unit,
  onConfirm: () -> Unit,
  confirmEnabled: Boolean,
  confirmLabel: String,
) {
  val strings = LocalStrings.current
  Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 16.dp)) {
    TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
      Text(
        strings.cancel,
        style = MaterialTheme.typography.bodyLargeEmphasized,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(
      title,
      modifier = Modifier.align(Alignment.Center),
      style = MaterialTheme.typography.titleMediumEmphasized,
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
    )
    TextButton(
      onClick = onConfirm,
      enabled = confirmEnabled,
      modifier = Modifier.align(Alignment.CenterEnd),
    ) {
      Text(
        confirmLabel,
        style = MaterialTheme.typography.bodyLargeEmphasized,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}
