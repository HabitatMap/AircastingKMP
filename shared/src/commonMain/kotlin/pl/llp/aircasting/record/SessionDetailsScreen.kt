package pl.llp.aircasting.record

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_add
import aircasting.shared.generated.resources.ic_cancel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.wizard.WizardButton
import pl.llp.aircasting.ui.wizard.WizardScaffold

@Composable
fun SessionDetailsScreen(
  state: NewSessionState,
  onRename: (String) -> Unit,
  onEditTags: (String) -> Unit,
  onAddTag: (String) -> Unit,
  onInterval: (SamplingInterval) -> Unit,
  onBack: () -> Unit,
  onCancel: () -> Unit,
  onNext: () -> Unit,
) {
  val strings = LocalStrings.current
  WizardScaffold(
    progress = state.step.progress,
    onBack = onBack,
    onCancel = onCancel,
    footer = { WizardButton(strings.next, onNext, enabled = state.canContinue) },
  ) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
      Text(
        strings.newSessionDetailsTitle,
        style = MaterialTheme.typography.headlineMediumEmphasized,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(Modifier.height(16.dp))
      Text(
        strings.newSessionDetailsBody,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(Modifier.height(24.dp))

      SessionField(
        value = state.form.name,
        onValueChange = onRename,
        label = strings.sessionNameLabel,
        placeholder = strings.sessionNamePlaceholder,
        imeAction = ImeAction.Next,
      )
      Spacer(Modifier.height(32.dp))
      SessionField(
        value = state.form.tagsInput,
        onValueChange = onEditTags,
        label = strings.sessionTagsLabel,
        placeholder = strings.sessionTagsPlaceholder,
        imeAction = ImeAction.Next,
      )

      if (state.recentTags.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        SectionHeader(strings.latestTagsHeader)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          state.recentTags.filterNot { it in state.form.tags }
            .forEach { tag ->
              SuggestionChip(
                onClick = { onAddTag(tag) },
                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                icon = {
                  Icon(
                    painterResource(Res.drawable.ic_add),
                    contentDescription = strings.addTagLabel(tag),
                    modifier = Modifier.size(18.dp),
                  )
                },
              )
            }
        }
      }
      Spacer(Modifier.height(24.dp))
      SectionHeader(strings.samplingIntervalHeader)
      Spacer(Modifier.height(8.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SamplingInterval.entries.forEach { interval ->
          IntervalChip(
            label = strings.label(interval),
            selected = interval == state.form.interval,
            onClick = { onInterval(interval) },
          )
        }
      }
      Spacer(Modifier.height(8.dp))
      Text(
        strings.samplingIntervalHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun RowScope.IntervalChip(label: String, selected: Boolean, onClick: () -> Unit) {
  val shape = RoundedCornerShape(10.dp)
  Surface(
    modifier = Modifier
      .weight(1f)
      .height(44.dp)
      .clip(shape)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    shape = shape,
    color = if (selected) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
    } else {
      MaterialTheme.colorScheme.surfaceContainerLowest
    },
    border = BorderStroke(
      1.5.dp,
      if (selected) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.outlineVariant
      },
    ),
  ) {
    Row(
      Modifier.fillMaxHeight().padding(horizontal = 6.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) {
          MaterialTheme.colorScheme.onPrimaryContainer
        } else {
          MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

@Composable
private fun SectionHeader(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.outline,
  )
}

@Composable
private fun SessionField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  imeAction: ImeAction,
) {
  val strings = LocalStrings.current
  val fieldState = rememberTextFieldState(value)
  val onEdit by rememberUpdatedState(onValueChange)
  LaunchedEffect(fieldState) {
    snapshotFlow { fieldState.text.toString() }.collect { onEdit(it) }
  }
  TextField(
    state = fieldState,
    modifier = Modifier.fillMaxWidth(),
    labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
    label = { Text(label) },
    placeholder = { Text(placeholder) },
    trailingIcon = {
      if (fieldState.text.isNotEmpty()) {
        IconButton(onClick = { fieldState.clearText() }) {
          Icon(
            painterResource(Res.drawable.ic_cancel),
            contentDescription = strings.customServerClearField,
            modifier = Modifier.size(24.dp),
          )
        }
      }
    },
    keyboardOptions = KeyboardOptions(imeAction = imeAction),
    lineLimits = TextFieldLineLimits.SingleLine,
    colors = TextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      focusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
      unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
    ),
  )
}