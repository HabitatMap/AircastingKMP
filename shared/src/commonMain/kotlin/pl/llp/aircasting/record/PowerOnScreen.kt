package pl.llp.aircasting.record

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_power_badge
import aircasting.shared.generated.resources.img_onboarding_airbeam
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.wizard.WizardButton
import pl.llp.aircasting.ui.wizard.WizardScaffold

/** Before scanning: the AirBeam must be on to advertise (legacy TurnOnAirBeamViewMvcImpl). */
@Composable
fun PowerOnScreen(
  onBack: () -> Unit,
  onCancel: () -> Unit,
  onNext: () -> Unit,
) {
  val strings = LocalStrings.current
  WizardScaffold(
    progress = NewSessionStep.PowerOn.progress,
    onBack = onBack,
    onCancel = onCancel,
    footer = { WizardButton(strings.next, onNext) },
  ) {
    Column(
      Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 96.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(Modifier.size(200.dp)) {
        Image(
          painterResource(Res.drawable.img_onboarding_airbeam),
          contentDescription = null,
          modifier = Modifier.size(200.dp),
        )
        // Figma pins the 60 dp badge at (140.5, 140) inside the 200 dp illustration.
        Image(
          painterResource(Res.drawable.ic_power_badge),
          contentDescription = null,
          modifier = Modifier.offset(x = 140.5.dp, y = 140.dp).size(60.dp),
        )
      }
      Spacer(Modifier.height(24.dp))
      Text(
        strings.powerOnAirBeamTitle,
        style = MaterialTheme.typography.headlineLargeEmphasized,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        strings.powerOnAirBeamBody,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}
