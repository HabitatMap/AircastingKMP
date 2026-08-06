package pl.llp.aircasting.settings.mic

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_add
import aircasting.shared.generated.resources.ic_remove
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.settings.SettingsSheet
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun MicCalibrationSheet(offset: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
  val strings = LocalStrings.current
  var draft by remember(offset) { mutableStateOf(offset) }

  val source = koinInject<MicrophoneLevelSource>()
  val permission = rememberMicrophonePermission()
  // Asked for on open, not behind a button: the sheet is useless without a reading, and the
  // system dialog only appears once per install anyway.
  LaunchedEffect(Unit) { if (!permission.granted) permission.request() }

  // Recreating the flow when the grant flips restarts capture the moment permission arrives;
  // `collectAsStateWithLifecycle` tears the old collection (and the AudioRecord) down for us.
  val levels: Flow<Double?> = remember(source, permission.granted) {
    if (permission.granted) source.levels() else emptyFlow()
  }
  val raw by levels.collectAsStateWithLifecycle(initialValue = null)
  val reading = raw?.let { calibrate(it, draft) }

  SettingsSheet(
    title = strings.appSettingMicrophoneCalibration,
    onDismiss = onDismiss,
    onConfirm = { onConfirm(draft) },
  ) {
    Column(
      Modifier.padding(horizontal = 16.dp)
        .padding(bottom = 48.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      LiveReadingCard(reading)
      Text(
        if (permission.granted) strings.micCalibrationHint else strings.micPermissionNeeded,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      OffsetStepper(draft, onChange = { draft = it })
    }
  }
}

@Composable
private fun LiveReadingCard(reading: Double?) {
  val strings = LocalStrings.current
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      strings.micLiveReading,
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.outline,
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Bottom,
    ) {
      Text(
        // An em-dash pair, not localised copy: it is punctuation standing in for a number.
        reading?.let(::formatReading) ?: "––",
        style = BigNumber,
        color = MaterialTheme.colorScheme.onBackground,
      )

      Text(
        strings.micDecibels,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.outline,
      )
    }
    Waveform(
      loudness = loudnessFraction(reading ?: 0.0),
      modifier = Modifier.fillMaxWidth()
        .height(40.dp),
    )
  }
}

private const val WaveformBars = 28
private val MinBarHeight = 5.dp
private val MaxBarHeight = 30.dp

@Composable
private fun Waveform(loudness: Float, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(2.5.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(WaveformBars) { i ->
      val jitter = 0.78f + 0.22f * abs(sin(i * 2.399f))
      val height by animateDpAsState(
        MinBarHeight + (MaxBarHeight - MinBarHeight) * loudness * jitter,
        label = "waveformBar$i",
      )
      Box(
        Modifier
          .width(3.dp)
          .height(height)
          .background(
            MaterialTheme.colorScheme.primaryContainer
              .copy(alpha = 0.25f + 0.72f * i / (WaveformBars - 1f)),
            CircleShape,
          ),
      )
    }
  }
}

@Composable
private fun OffsetStepper(offset: Int, onChange: (Int) -> Unit) {
  val strings = LocalStrings.current
  Column(
    Modifier.fillMaxWidth()
      .padding(top = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      strings.micCalibrationOffset,
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.outline,
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StepButton(
        icon = Res.drawable.ic_remove,
        label = strings.micDecreaseOffset,
        background = MaterialTheme.colorScheme.surfaceContainerHighest,
        tint = MaterialTheme.colorScheme.onSurface,
        enabled = offset > CalibrationOffsetRange.first,
      ) { onChange(offset.steppedBy(-1)) }
      Text(
        offset.toString(),
        modifier = Modifier.width(88.dp),
        style = BigNumber,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      StepButton(
        icon = Res.drawable.ic_add,
        label = strings.micIncreaseOffset,
        background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f),
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        enabled = offset < CalibrationOffsetRange.last,
      ) { onChange(offset.steppedBy(1)) }
    }
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = { onChange(DefaultCalibrationOffset) }) {
      Text(
        strings.micResetOffset(DefaultCalibrationOffset),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}

@Composable
private fun StepButton(
  icon: DrawableResource,
  label: String,
  background: Color,
  tint: Color,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Box(
    Modifier
      .size(48.dp)
      .clip(CircleShape)
      .background(background)
      .clickable(enabled = enabled, onClickLabel = label, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painterResource(icon),
      contentDescription = label,
      modifier = Modifier.size(18.dp),
      tint = if (enabled) tint else tint.copy(alpha = 0.38f),
    )
  }
}

private val BigNumber: TextStyle
  @Composable get() = MaterialTheme.typography.headlineLarge.copy(
    fontSize = 34.sp,
    lineHeight = 41.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.4.sp,
  )
