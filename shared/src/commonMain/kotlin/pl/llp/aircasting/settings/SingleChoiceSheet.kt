package pl.llp.aircasting.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.app.ChoiceOption

@Composable
fun <T> SingleChoiceSheet(
  title: String,
  options: List<ChoiceOption<T>>,
  selected: T,
  onDismiss: () -> Unit,
  onConfirm: (T) -> Unit,
) {
  // Keyed on the committed value so reopening the sheet after a change starts from the new one
  // rather than resurrecting the previous draft.
  var draft by remember(selected) { mutableStateOf(selected) }
  SettingsSheet(title, onDismiss = onDismiss, onConfirm = { onConfirm(draft) }) {
    Column(
      Modifier
        .padding(horizontal = 16.dp)
        .padding(bottom = 48.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .padding(horizontal = 16.dp)
        // Reports the rows to screen readers as one group of N, not N unrelated radios.
        .selectableGroup(),
    ) {
      options.forEach { option ->
        ChoiceRow(option, selected = option.value == draft) { draft = option.value }
      }
    }
  }
}

@Composable
private fun <T> ChoiceRow(option: ChoiceOption<T>, selected: Boolean, onSelect: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
      .heightIn(min = 48.dp)
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(Modifier.weight(1f)) {
      Text(
        option.label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      option.supporting?.let {
        Text(
          it,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    RadioButton(
      selected = selected,
      onClick = null,
      colors = RadioButtonDefaults.colors(
        selectedColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedColor = MaterialTheme.colorScheme.outline,
      ),
    )
  }
}
