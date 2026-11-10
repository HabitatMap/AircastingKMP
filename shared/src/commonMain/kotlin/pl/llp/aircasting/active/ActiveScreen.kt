package pl.llp.aircasting.active

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import aircasting.shared.generated.resources.ic_bluetooth_disabled
import aircasting.shared.generated.resources.ic_stop_circle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.components.DotSeparatedRow
import pl.llp.aircasting.ui.components.LevelBadge
import pl.llp.aircasting.ui.components.LevelDot
import pl.llp.aircasting.ui.components.SectionHeader
import pl.llp.aircasting.ui.components.StreamChip
import pl.llp.aircasting.ui.components.TintedTag
import pl.llp.aircasting.ui.theme.LocalAqColors
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun ActiveScreen(
  state: ActiveUiState,
  onSelectStream: (uuid: String, sensorName: String) -> Unit,
  onFinish: (uuid: String) -> Unit,
  onConfirmFinish: () -> Unit,
  onDismissFinish: () -> Unit,
  onStartMobileSession: () -> Unit,
) {
  val strings = LocalStrings.current
  val clock = koinInject<Clock>()
  // Re-read on every state: a recording session re-emits each second, so "x min ago" stays fresh.
  val now = remember(state) { clock.now() }
  var mobileExpanded by rememberSaveable { mutableStateOf(true) }
  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 48.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item {
      SectionHeader(
        strings.activeMobileSessions,
        state.mobile.size,
        mobileExpanded
      ) { mobileExpanded = !mobileExpanded }
    }
    if (mobileExpanded) {
      if (state.mobile.isEmpty()) {
        item(key = "mobile-empty") {
          EmptySectionCard(
            strings.activeNoMobileTitle,
            strings.activeNoMobileBody,
            strings.activeStartMobile,
            onStartMobileSession
          )
        }
      } else {
        items(state.mobile, key = { it.uuid }) { card ->
          MobileSessionCard(
            card,
            now,
            onSelectStream = { onSelectStream(card.uuid, it) },
            onFinish = { onFinish(card.uuid) },
          )
        }
      }
    }
  }

  state.confirmingFinish?.let { card ->
    FinishDialog(card.name, onConfirm = onConfirmFinish, onDismiss = onDismissFinish)
  }
}

@Composable
private fun SectionHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onToggle),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      val empty = count == 0
      Text(
        count.toString(),
        Modifier.clip(CircleShape)
          .background(
            if (empty) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
          )
          .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = if (empty) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
    Icon(
      painterResource(Res.drawable.ic_arrow_forward_ios),
      contentDescription = null,
      // The asset points right: up when open, down when folded.
      Modifier.size(20.dp).rotate(if (expanded) -90f else 90f),
      tint = MaterialTheme.colorScheme.primary,
    )
  }
}


@Composable
private fun MobileSessionCard(
  card: ActiveSessionCard,
  now: Instant,
  onSelectStream: (sensorName: String) -> Unit,
  onFinish: () -> Unit,
) {
  val strings = LocalStrings.current
  Column(
    Modifier.fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    CardHeader(card, now)
    when (card.state) {
      CardState.Live -> StreamPicker(card, onSelectStream)
      CardState.NoDataYet -> {
        StreamPicker(card, onSelectStream)
        Waiting(strings.activeNoDataTitle, strings.activeNoDataBody, spinner = 72.dp)
      }
      CardState.ConnectionLost -> Waiting(strings.activeLookingTitle, strings.activeLookingBody, spinner = 32.dp)
    }
    Footer(card.state, onFinish)
  }
}

@Composable
private fun CardHeader(card: ActiveSessionCard, now: Instant) {
  val strings = LocalStrings.current
  Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      TintedTag(strings.activeTagAirBeamMobile)
      when (card.state) {
        CardState.Live -> card.selected?.level?.let { LevelBadge(it) }
        CardState.NoDataYet -> Unit // Figma 1009:28154: nothing to grade yet
        CardState.ConnectionLost -> ConnectionLostBadge()
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(card.name, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onSurface)
        DotSeparatedRow(card.meta(now, strings), MaterialTheme.typography.bodySmall, MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (card.tags.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          card.tags.forEach { UserTag(it) }
        }
      }
    }
  }
}

/** Same pill shape as [LevelBadge], in the "unhealthy" red, with the Bluetooth-off glyph. */
@Composable
private fun ConnectionLostBadge() {
  val red = LocalAqColors.current.unhealthy
  Row(
    Modifier.clip(RoundedCornerShape(100.dp)).background(red.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(painterResource(Res.drawable.ic_bluetooth_disabled), contentDescription = null, Modifier.size(12.dp), tint = red)
    Text(LocalStrings.current.activeConnectionLost, style = MaterialTheme.typography.labelSmall, color = red)
  }
}

/** Figma "UserTag": hairline-outlined pill. */
@Composable
private fun UserTag(tag: String) {
  Text(
    tag,
    Modifier.border(0.65.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

/** Stream chips, then the selected stream's newest value with its level dot. */
@Composable
private fun StreamPicker(card: ActiveSessionCard, onSelectStream: (String) -> Unit) {
  val strings = LocalStrings.current
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      card.streams.forEach { stream ->
        StreamChip(
          chipLabel(stream.sensorName, strings),
          selected = stream.sensorName == card.selected?.sensorName,
          onClick = { onSelectStream(stream.sensorName) },
        )
      }
    }

    card.selected?.let { stream ->
      Row(
        Modifier.padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // No level → grey dot, as Figma 1009:28154 draws it.
        stream.level?.let { LevelDot(it) }
          ?: Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            valueText(stream.value),
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            stream.unitSymbol,
            Modifier.alignByBaseline(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
          )
        }
      }
    }
  }
}

@Composable
private fun Waiting(title: String, body: String, spinner: Dp) {
  Column(
    Modifier.fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.surface)
      .padding(horizontal = 10.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    CircularProgressIndicator(Modifier.size(spinner))
    Column(
      Modifier.padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
      Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
  }
}

@Composable
private fun EmptySectionCard(title: String, body: String, action: String, onAction: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Column(
    Modifier.fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(colors.surfaceContainerLowest)
      .padding(horizontal = 24.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
      Text(body, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
    }
    CardButton(action, onAction, colors.primary, colors.onSurface, Modifier.fillMaxWidth())
  }
}

@Composable
private fun Footer(state: CardState, onFinish: () -> Unit) {
  val strings = LocalStrings.current
  val colors = MaterialTheme.colorScheme
  Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    HorizontalDivider(color = colors.surfaceContainerHighest)
    if (state == CardState.ConnectionLost) {
      // No SD sync yet: the button is there, and does nothing (decided 2026-11-08).
      CardButton(strings.activeFinishAndSync, onClick = {}, colors.error, colors.error, Modifier.fillMaxWidth(), Res.drawable.ic_stop_circle)
      TextButton(onClick = onFinish) {
        Text(strings.activeFinishAndDiscard, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
      }
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CardButton(strings.activeFinishRecording, onFinish, colors.error, colors.error, Modifier.weight(1f), Res.drawable.ic_stop_circle)
        // Session details screen comes later.
        CardButton(strings.activeSeeDetails, onClick = {}, colors.primary, colors.onSurface, Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun CardButton(
  label: String,
  onClick: () -> Unit,
  borderColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  icon: DrawableResource? = null,
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.2.dp, borderColor),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      contentColor = contentColor,
    ),

    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
  ) {
    icon?.let {
      Icon(painterResource(it), contentDescription = null, Modifier.size(16.dp))
      Spacer(Modifier.width(6.dp))
    }
    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
  }
}
@Composable
private fun FinishDialog(sessionName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  val strings = LocalStrings.current
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    title = { Text(strings.activeFinishTitle(sessionName)) },
    text = { Text(strings.activeFinishBody) },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
      ) { Text(strings.activeFinishSession) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.activeKeepRecording) } },
  )
}
