package pl.llp.aircasting.record

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.wizard.WizardButton
import pl.llp.aircasting.ui.wizard.WizardScaffold

@Composable
fun SessionConfirmScreen(
  state: NewSessionState,
  onBack: () -> Unit,
  onCancel: () -> Unit,
  onStart: () -> Unit,
) {
  val strings = LocalStrings.current
  val summary = strings.summarize(state.form)
  WizardScaffold(
    progress = state.step.progress,
    onBack = onBack,
    onCancel = onCancel,
    footer = { WizardButton(strings.startRecording, onStart, showChevron = false) },
  ) {
    Column {
      Text(
        strings.newSessionConfirmTitle,
        style = MaterialTheme.typography.headlineMediumEmphasized,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(Modifier.height(12.dp))
      Text(
        state.form.method?.let { strings.copyFor(it).confirmBody }.orEmpty(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(24.dp))
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
      ) {
        Column(
          Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          SummaryRow(strings.summaryTypeLabel) { SummaryValue(summary.type) }
          SummaryDivider()
          SummaryRow(strings.summaryNameLabel) { SummaryValue(summary.name) }
          if (summary.tags.isNotEmpty()) {
            SummaryDivider()
            SummaryRow(strings.summaryTagsLabel) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.tags.forEach { SummaryTag(it) }
              }
            }
          }
          SummaryDivider()
          SummaryRow(strings.summaryIntervalLabel) { SummaryValue(summary.interval) }
        }
      }
    }
  }
}

@Composable
private fun SummaryRow(label: String, value: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    value()
  }
}

@Composable
private fun SummaryValue(value: String) {
  Text(
    value,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onBackground,
  )
}

@Composable
private fun SummaryDivider() {
  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SummaryTag(tag: String) {
  Text(
    tag,
    modifier = Modifier
      .background(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
        RoundedCornerShape(percent = 50),
      )
      .border(1.2.dp, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 50))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onPrimaryContainer,
  )
}