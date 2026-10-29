package pl.llp.aircasting.record

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_add
import aircasting.shared.generated.resources.ic_cancel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.components.SectionHeader
import pl.llp.aircasting.ui.components.SelectableChip
import pl.llp.aircasting.ui.wizard.WizardButton
import pl.llp.aircasting.ui.wizard.WizardScaffold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionDetailsScreen(
  state: NewSessionState,
  onRename: (String) -> Unit,
  onEditTagDraft: (String) -> Unit,
  onCommitTag: () -> Unit,
  onAddTag: (String) -> Unit,
  onRemoveTag: (String) -> Unit,
  onIndoor: (Boolean) -> Unit,
  onWifiName: (String) -> Unit,
  onWifiPassword: (String) -> Unit,
  wifiNetworks: WifiNetworks?,
  onInterval: (SamplingInterval) -> Unit,
  onBack: () -> Unit,
  onCancel: () -> Unit,
  onNext: () -> Unit,
) {
  val strings = LocalStrings.current
  val form = state.form
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
        value = form.name,
        onValueChange = onRename,
        label = strings.sessionNameLabel,
        placeholder = strings.sessionNamePlaceholder,
        imeAction = ImeAction.Next,
      )
      Spacer(Modifier.height(32.dp))
      TagsField(form.committedTags, form.tagDraft, onEditTagDraft, onCommitTag, onRemoveTag)

      if (form.tagDraft.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        SectionHeader(strings.matchingTagsHeader)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          state.matchingTags.forEach { tag -> TagSuggestion(tag, strings.addTagLabel(tag)) { onAddTag(tag) } }
          TagSuggestion(strings.addNewTag(form.tagDraft), description = null, onClick = onCommitTag)
        }
      } else if (state.recentTags.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        SectionHeader(strings.latestTagsHeader)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          state.recentTags.filterNot { it in form.tags }
            .forEach { tag -> TagSuggestion(tag, strings.addTagLabel(tag)) { onAddTag(tag) } }
        }
      }

      if (form.method == RecordingMethod.AirBeamFixed) {
        Spacer(Modifier.height(24.dp))
        SectionHeader(strings.placementHeader)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
          listOf(true, false).forEachIndexed { index, indoor ->
            SegmentedButton(
              selected = form.indoor == indoor,
              onClick = { onIndoor(indoor) },
              shape = SegmentedButtonDefaults.itemShape(index, count = 2),
            ) { Text(if (indoor) strings.placementIndoor else strings.placementOutdoor) }
          }
        }
        Spacer(Modifier.height(8.dp))
        Hint(strings.placementHint)

        Spacer(Modifier.height(24.dp))
        SectionHeader(strings.wifiHeader)
        Spacer(Modifier.height(8.dp))
        SessionField(
          value = form.wifi.ssid,
          onValueChange = onWifiName,
          label = strings.wifiNameLabel,
          placeholder = strings.wifiNamePlaceholder,
          imeAction = ImeAction.Next,
          hint = strings.wifiNameHint,
        )
        if (wifiNetworks != null) {
          Spacer(Modifier.height(8.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!wifiNetworks.granted) {
              TagSuggestion(strings.wifiShowNearby, description = null, onClick = wifiNetworks::request)
            }
            wifiNetworks.names.filterNot { it == form.wifi.ssid }.forEach { name ->
              SuggestionChip(onClick = { onWifiName(name) }, label = { Text(name, style = MaterialTheme.typography.labelSmall) })
            }
          }
        }
        Spacer(Modifier.height(16.dp))
        PasswordField(
          value = form.wifi.password,
          onValueChange = onWifiPassword,
          label = strings.wifiPasswordLabel,
          placeholder = strings.wifiPasswordPlaceholder,
          hint = strings.wifiPasswordHint,
        )
      } else {
        // Fixed sessions always sample every 60 s (DOMAIN_KNOWLEDGE.md), so only mobile picks.
        Spacer(Modifier.height(24.dp))
        SectionHeader(strings.samplingIntervalHeader)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SamplingInterval.entries.forEach { interval ->
            SelectableChip(
              label = strings.label(interval),
              selected = interval == form.interval,
              onClick = { onInterval(interval) },
              modifier = Modifier.weight(1f),
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Hint(strings.samplingIntervalHint)
      }
    }
  }
}

/**
 * Chips plus the word being typed, in one wrapping row. The ViewModel moves finished words into
 * chips, so unlike [SessionField] this field also follows [draft] back from it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsField(
  chips: List<String>,
  draft: String,
  onDraft: (String) -> Unit,
  onCommit: () -> Unit,
  onRemove: (String) -> Unit,
) {
  val strings = LocalStrings.current
  val colors = MaterialTheme.colorScheme
  val fieldState = rememberReportingFieldState(draft, onDraft)
  LaunchedEffect(draft) {
    if (fieldState.text.toString() != draft) fieldState.setTextAndPlaceCursorAtEnd(draft)
  }
  Column(
    Modifier
      .fillMaxWidth()
      .background(colors.surfaceContainerLowest)
      .padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Text(strings.sessionTagsLabel, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      chips.forEach { tag ->
        InputChip(
          selected = false,
          onClick = { onRemove(tag) },
          label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
          trailingIcon = {
            Icon(
              painterResource(Res.drawable.ic_cancel),
              contentDescription = strings.removeTagLabel(tag),
              modifier = Modifier.size(18.dp),
            )
          },
          shape = CircleShape,
        )
      }
      BasicTextField(
        state = fieldState,
        modifier = Modifier.widthIn(min = 96.dp).align(Alignment.CenterVertically),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { onCommit() },
        decorator = { innerField ->
          Box {
            if (fieldState.text.isEmpty() && chips.isEmpty()) {
              Text(strings.sessionTagsPlaceholder, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            innerField()
          }
        },
      )
    }
  }
  HorizontalDivider(color = colors.outlineVariant)
}

@Composable
private fun TagSuggestion(label: String, description: String?, onClick: () -> Unit) {
  SuggestionChip(
    onClick = onClick,
    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    icon = {
      Icon(painterResource(Res.drawable.ic_add), contentDescription = description, modifier = Modifier.size(18.dp))
    },
  )
}

@Composable
private fun SessionField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  imeAction: ImeAction,
  hint: String? = null,
) {
  val strings = LocalStrings.current
  val fieldState = rememberReportingFieldState(value, onValueChange)
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
    supportingText = hint?.let { { Text(it) } },
    keyboardOptions = KeyboardOptions(imeAction = imeAction),
    lineLimits = TextFieldLineLimits.SingleLine,
    colors = sessionFieldColors(),
  )
}

/** Masked, and its keyboard is a password keyboard: no suggestions, no learning what was typed. */
@Composable
private fun PasswordField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String,
  hint: String,
) {
  SecureTextField(
    state = rememberReportingFieldState(value, onValueChange),
    modifier = Modifier.fillMaxWidth(),
    labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
    label = { Text(label) },
    placeholder = { Text(placeholder) },
    supportingText = { Text(hint) },
    colors = sessionFieldColors(),
  )
}

/** Seeded from [value], reports every edit to [onValueChange], and follows [value] when it changes from outside. */
@Composable
private fun rememberReportingFieldState(value: String, onValueChange: (String) -> Unit): TextFieldState {
  val fieldState = rememberTextFieldState(value)
  val onEdit by rememberUpdatedState(onValueChange)
  LaunchedEffect(fieldState) {
    snapshotFlow { fieldState.text.toString() }.collect { onEdit(it) }
  }
  LaunchedEffect(value) {
    if (fieldState.text.toString() != value) fieldState.setTextAndPlaceCursorAtEnd(value)
  }
  return fieldState
}

@Composable
private fun sessionFieldColors(): TextFieldColors = TextFieldDefaults.colors(
  focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
  unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
  focusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
  unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
)

@Composable
private fun Hint(text: String) {
  Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}