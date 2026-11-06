package pl.llp.aircasting.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.invalidBounds
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.SettingsSheet
import pl.llp.aircasting.ui.theme.AqBandColors

private val FieldShape = RoundedCornerShape(10.dp)

/**
 * Figma "Thresholds" (1913:518): type all five bounds, or reset the sensor to the server default.
 * Reset only refills the fields with [default]; nothing is stored until Save. A valid Save animates
 * the sheet out, then calls [onDismiss]. [default] null = no server default → no Reset button.
 */
@Composable
fun ThresholdsSheet(
  threshold: SensorThreshold,
  default: SensorThreshold?,
  onDismiss: () -> Unit,
  onSave: (SensorThreshold) -> Unit,
) {
  val strings = LocalStrings.current
  // Text, not Int: mid-edit a field can be blank or "-". Index 0 = Min … 4 = Max.
  var fields by remember(threshold) { mutableStateOf(threshold.bounds.map(Int::toString)) }
  // Errors only after a failed Save, then live, so a number isn't red while it's being typed.
  var showErrors by remember { mutableStateOf(false) }
  val invalid = if (showErrors) invalidBounds(fields) else emptySet()
  var closing by remember { mutableStateOf(false) }

  SettingsSheet(
    title = strings.thresholdsTitle,
    onDismiss = onDismiss,
    onConfirm = {
      val saved = threshold.withBounds(fields)
      if (saved == null) showErrors = true
      else { onSave(saved); closing = true }
    },
    confirmLabel = strings.save,
    closing = closing,
  ) {
    Column(
      Modifier.padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        strings.thresholdsHint,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      ThresholdLadder(threshold, fields, invalid) { index, text ->
        fields = fields.toMutableList().also { it[index] = text }
      }
      if (default != null) TextButton(
        onClick = {
          fields = default.bounds.map(Int::toString)
          showErrors = false
        },
        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
      ) {
        Text(
          strings.thresholdsReset,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
  }
}

/** White card: colour rail on the left, one labelled field per bound, Max on top. */
@Composable
private fun ThresholdLadder(
  threshold: SensorThreshold,
  fields: List<String>,
  invalid: Set<Int>,
  onFieldChange: (index: Int, text: String) -> Unit,
) {
  val strings = LocalStrings.current
  val labels = listOf(strings.thresholdMin, strings.thresholdLow, strings.thresholdMedium, strings.thresholdHigh, strings.thresholdMax)
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(horizontal = 16.dp, vertical = 26.dp)
      // Lets the rail's fillMaxHeight match the fields column, whatever the font scale.
      .height(IntrinsicSize.Min),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // A key, not to scale: one equal step per band, very high on top.
    Column(Modifier.width(10.dp).fillMaxHeight().clip(RoundedCornerShape(5.dp))) {
      threshold.bands.asReversed().forEach { (_, level) ->
        Box(Modifier.weight(1f).fillMaxWidth().background(AqBandColors.forLevel(level)))
      }
    }
    // Composed top to bottom, so the keyboard's Next walks Max → Min.
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
      for (i in fields.indices.reversed()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Text(
            labels[i],
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          ThresholdField(fields[i], { onFieldChange(i, it) }, label = labels[i], isError = i in invalid)
        }
      }
    }
  }
}

/** Figma "Field / Threshold value" (1903:528): 96×44 numeric box. States: Default, Focused, Error. */
@Composable
private fun ThresholdField(value: String, onValueChange: (String) -> Unit, label: String, isError: Boolean) {
  val colors = MaterialTheme.colorScheme
  val errorText = LocalStrings.current.thresholdsInvalid
  val interaction = remember { MutableInteractionSource() }
  val focused by interaction.collectIsFocusedAsState()
  val borderColor = when {
    isError -> colors.error
    focused -> colors.primaryContainer
    else -> colors.outlineVariant
  }
  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = Modifier
      .size(width = 96.dp, height = 44.dp)
      .background(colors.surfaceContainerLowest, FieldShape)
      .border(1.dp, borderColor, FieldShape)
      // The visible label is a sibling Text; tell screen readers which bound this is, and why it's red.
      .semantics {
        contentDescription = label
        if (isError) error(errorText)
      },
    textStyle = MaterialTheme.typography.titleSmallEmphasized.copy(color = colors.onSurface, textAlign = TextAlign.Center),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    singleLine = true,
    cursorBrush = SolidColor(colors.primaryContainer),
    interactionSource = interaction,
    decorationBox = { inner ->
      Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { inner() }
    },
  )
}
