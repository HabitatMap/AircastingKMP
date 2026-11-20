package pl.llp.aircasting.record

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_cancel
import aircasting.shared.generated.resources.ic_check
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.components.LoadingSpinner
import pl.llp.aircasting.ui.wizard.WizardButton
import pl.llp.aircasting.ui.wizard.WizardScaffold

/**
 * After the device picker: connecting to the picked AirBeam, then connected or failed
 * (legacy ConnectingAirBeamFragment / AirBeamConnectedFragment). [onNext] carries on to the
 * session details; [onRetry] goes back to the picker.
 */
@Composable
fun ConnectScreen(
  state: ConnectState,
  onBack: () -> Unit,
  onCancel: () -> Unit,
  onNext: () -> Unit,
  onRetry: () -> Unit,
) {
  val strings = LocalStrings.current
  WizardScaffold(
    progress = NewSessionStep.Connect.progress,
    onBack = onBack,
    onCancel = onCancel,
    footer = {
      when (state) {
        ConnectState.Connected -> WizardButton(strings.next, onNext)
        ConnectState.Failed -> WizardButton(strings.tryAgain, onRetry, showChevron = false)
        ConnectState.Idle, ConnectState.Connecting -> Unit
      }
    },
  ) {
    Column(
      // Figma: 140 dp below the nav row, minus the scaffold's own 24 dp spacing.
      Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 116.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        when (state) {
          ConnectState.Idle, ConnectState.Connecting -> LoadingSpinner()
          ConnectState.Connected ->
            OutcomeBadge(painterResource(Res.drawable.ic_check), PrimaryFixed, OnPrimaryFixedVariant)
          ConnectState.Failed -> OutcomeBadge(
            painterResource(Res.drawable.ic_cancel),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            // ic_cancel's glyph fills its box; Figma's 32 dp symbol insets it to 26.67 dp.
            glyphSize = 26.67f,
          )
        }
      }
      Spacer(Modifier.height(24.dp))
      Text(
        when (state) {
          ConnectState.Connected -> strings.airBeamConnectedTitle
          ConnectState.Failed -> strings.airBeamConnectFailedTitle
          else -> strings.connectingAirBeamTitle
        },
        style = MaterialTheme.typography.headlineLargeEmphasized,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        when (state) {
          ConnectState.Connected -> strings.airBeamConnectedBody
          ConnectState.Failed -> strings.airBeamConnectFailedBody
          else -> strings.connectingAirBeamBody
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

/** Figma's 72 dp tinted circle with a 32 dp symbol and a soft drop shadow. */
@Composable
private fun OutcomeBadge(icon: Painter, background: Color, tint: Color, glyphSize: Float = 32f) {
  Box(
    Modifier
      .size(72.dp)
      .dropShadow(CircleShape, BadgeShadow)
      .background(background, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(glyphSize.dp), tint = tint)
  }
}

private val BadgeShadow = Shadow(radius = 6.dp, color = Color(0x1A000000), offset = DpOffset(0.dp, 4.dp))

// M3 "fixed" roles keep one value in light and dark, so they're not in the theme's schemes.
private val PrimaryFixed = Color(0xFFC4E7FF)
private val OnPrimaryFixedVariant = Color(0xFF004C69)
