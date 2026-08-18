package pl.llp.aircasting.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.wizard.WizardButton
import pl.llp.aircasting.ui.wizard.WizardScaffold

@Composable
fun RecordMethodScreen(
  selected: RecordingMethod?,
  onSelect: (RecordingMethod) -> Unit,
  onBack: () -> Unit,
  onCancel: () -> Unit,
  onNext: () -> Unit,
) {
  val strings = LocalStrings.current
  WizardScaffold(
    progress = NewSessionStep.Method.progress,
    onBack = onBack,
    onCancel = onCancel,
    footer = { WizardButton(strings.next, onNext, enabled = selected != null) },
  ) {
    Column {
      Text(
        strings.newSessionMethodTitle,
        style = MaterialTheme.typography.headlineMediumEmphasized,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(Modifier.height(20.dp))
      Text(
        strings.newSessionMethodBody,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(Modifier.height(32.dp))
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RecordingMethod.entries.forEach { method ->
          MethodCard(
            copy = strings.copyFor(method),
            selected = method == selected,
            onSelect = { onSelect(method) },
          )
        }
      }
    }
  }
}

@Composable
private fun MethodCard(copy: RecordingMethodCopy, selected: Boolean, onSelect: () -> Unit) {
  val shape = RoundedCornerShape(22.dp)
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
  ) {
    Row(
      Modifier.padding(20.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          copy.tag,
          modifier = Modifier
            .background(
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
              RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.height(8.dp))
        Text(
          copy.title,
          style = MaterialTheme.typography.titleMediumEmphasized,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          copy.body,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
      RadioButton(
        selected = selected,
        onClick = null,
        modifier = Modifier.align(Alignment.CenterVertically),
        colors = RadioButtonDefaults.colors(
          selectedColor = MaterialTheme.colorScheme.primaryContainer,
          unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
      )
    }
  }
}