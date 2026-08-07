package pl.llp.aircasting.settings.server

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_cancel
import aircasting.shared.generated.resources.ic_check
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.llp.aircasting.i18n.LocalStrings

@Composable
fun CustomDataServerScreen(
  step: CustomServerStep,
  onBack: () -> Unit,
  onExit: () -> Unit,
  onEdit: (CustomServerForm) -> Unit,
  onNext: () -> Unit,
  onUseOfficial: () -> Unit,
  onSave: () -> Unit,
  onCancelTest: () -> Unit,
) {
  val strings = LocalStrings.current
  CustomDataServerScaffold(
    progress = step.progress,
    onBack = onBack,
    onCancel = onExit,
    footer = {
      when (step) {
        CustomServerStep.Intro -> WizardButton(strings.customServerNext, onNext)
        is CustomServerStep.Form ->
          WizardButton(strings.customServerNext, onNext, enabled = step.form.baseUrl != null)
        is CustomServerStep.Testing -> if (step.tookTooLong) {
          Text(
            strings.customServerTooLong,
            modifier = Modifier.clickable(onClick = onCancelTest).padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
        is CustomServerStep.Verified ->
          WizardButton(strings.customServerSave, onSave, showChevron = false)
        is CustomServerStep.Failed ->
          WizardButton(strings.customServerEdit, onBack, showChevron = false)
      }
    },
  ) {
    when (step) {
      CustomServerStep.Intro -> IntroStep()
      is CustomServerStep.Form -> FormStep(step.form, onEdit, onUseOfficial)
      is CustomServerStep.Testing -> StatusStep(
        title = strings.customServerTestingTitle,
        description = strings.customServerTestingBody,
        icon = { SpinnerBadge() },
      )
      is CustomServerStep.Verified -> StatusStep(
        title = strings.customServerVerifiedTitle,
        description = strings.customServerVerifiedBody(step.baseUrl),
        icon = {
          StatusBadge(
            Res.drawable.ic_check,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
          )
        },
      )
      is CustomServerStep.Failed -> StatusStep(
        title = strings.customServerFailedTitle,
        description = strings.customServerFailedBody(step.baseUrl),
        icon = {
          StatusBadge(
            Res.drawable.ic_cancel,
            tint = MaterialTheme.colorScheme.error,
            background = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
          )
        },
      )
    }
  }
}