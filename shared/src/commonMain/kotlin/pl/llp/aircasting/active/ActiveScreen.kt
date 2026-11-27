package pl.llp.aircasting.active

import aircasting.shared.generated.resources.Res
import aircasting.shared.generated.resources.ic_arrow_forward_ios
import aircasting.shared.generated.resources.ic_bluetooth_disabled
import aircasting.shared.generated.resources.ic_stop_circle
import aircasting.shared.generated.resources.ic_wifi_alert
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import pl.llp.aircasting.ui.components.ConfirmDialog
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.ui.components.CardButton
import pl.llp.aircasting.ui.components.CountBadge
import pl.llp.aircasting.ui.components.DotSeparatedRow
import pl.llp.aircasting.ui.components.LevelBadge
import pl.llp.aircasting.ui.components.LevelDot
import pl.llp.aircasting.ui.components.SectionHeader
import pl.llp.aircasting.ui.components.StreamChip
import pl.llp.aircasting.ui.components.TintedTag
import pl.llp.aircasting.ui.components.UserTag
import pl.llp.aircasting.ui.format.LocalDateFormatter
import pl.llp.aircasting.ui.theme.LocalAqColors
import pl.llp.aircasting.ui.theme.LocalAqBadgeColors
import kotlin.time.Clock
import kotlin.time.Instant
import pl.llp.aircasting.ui.components.LoadingSpinner

@Composable
fun ActiveScreen(
  state: ActiveUiState,
  onSelectStream: (uuid: String, sensorName: String) -> Unit,
  onFinish: (uuid: String) -> Unit,
  onConfirmFinish: () -> Unit,
  onDismissFinish: () -> Unit,
  onStartMobileSession: () -> Unit,
  onStartFixedSession: () -> Unit,
  onStartMicSession: () -> Unit,
  onSeeDetails: (uuid: String, sensorName: String?) -> Unit,
  /** Fixed sessions' details sync from the backend and map one dot: a different destination. */
  onSeeFixedDetails: (uuid: String, sensorName: String?) -> Unit,
  onReconnect: () -> Unit,
) {
  val strings = LocalStrings.current
  val clock = koinInject<Clock>()
  // Re-read on every state: a recording session re-emits each second, so "x min ago" stays fresh.
  val now = remember(state) { clock.now() }
  var mobileExpanded by rememberSaveable { mutableStateOf(true) }
  var fixedExpanded by rememberSaveable { mutableStateOf(true) }
  var micExpanded by rememberSaveable { mutableStateOf(true) }
  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 48.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    sessionSection(
      key = "mobile",
      title = strings.activeMobileSessions,
      cards = state.mobile,
      expanded = mobileExpanded,
      onToggle = { mobileExpanded = !mobileExpanded },
      empty = {
        EmptySectionCard(
          strings.activeNoMobileTitle,
          strings.activeNoMobileBody,
          strings.activeStartMobile,
          onStartMobileSession
        )
      },
    ) { card ->
      SessionCard(
        card, now, strings.activeTagAirBeamMobile, strings.activeNoDataBody,
        onSelectStream = { onSelectStream(card.uuid, it) },
        onFinish = { onFinish(card.uuid) },
        onSeeDetails = { onSeeDetails(card.uuid, card.selected?.sensorName) },
      )
    }
    sessionSection(
      key = "fixed",
      title = strings.activeFixedSessions,
      cards = state.fixed,
      expanded = fixedExpanded,
      onToggle = { fixedExpanded = !fixedExpanded },
      empty = {
        EmptySectionCard(
          strings.activeNoFixedTitle,
          strings.activeNoFixedBody,
          strings.activeStartFixed,
          onStartFixedSession
        )
      },
    ) { card ->
      SessionCard(
        card, now, strings.activeTagAirBeamFixed, strings.activeNoDataBody,
        onSelectStream = { onSelectStream(card.uuid, it) },
        onFinish = { onFinish(card.uuid) },
        onSeeDetails = { onSeeFixedDetails(card.uuid, card.selected?.sensorName) },
        onReconnect = onReconnect,
      )
    }
    sessionSection(
      key = "mic",
      title = strings.activePhoneMicrophone,
      cards = state.microphone,
      expanded = micExpanded,
      onToggle = { micExpanded = !micExpanded },
      empty = {
        EmptySectionCard(
          strings.activeNoMicTitle,
          strings.activeNoMicBody,
          strings.activeStartMic,
          onStartMicSession
        )
      },
    ) { card ->
      SessionCard(
        card, now, strings.activeTagPhoneMic, strings.activeNoMicDataBody,
        onSelectStream = { onSelectStream(card.uuid, it) },
        onFinish = { onFinish(card.uuid) },
        onSeeDetails = { onSeeDetails(card.uuid, card.selected?.sensorName) },
      )
    }
  }

  state.confirmingFinish?.let { card ->
    FinishDialog(card.name, onConfirm = onConfirmFinish, onDismiss = onDismissFinish)
  }
}

/** Header with count, then the cards — or the empty card when there are none (Figma 1137:27447). */
private fun LazyListScope.sessionSection(
  key: String,
  title: String,
  cards: List<ActiveSessionCard>,
  expanded: Boolean,
  onToggle: () -> Unit,
  empty: @Composable () -> Unit,
  card: @Composable (ActiveSessionCard) -> Unit,
) {
  item(key = "$key-header") { SectionHeader(title, cards.size, expanded, onToggle) }
  if (!expanded) return
  if (cards.isEmpty()) item(key = "$key-empty") { empty() }
  else items(cards, key = { it.uuid }) { card(it) }
}

@Composable
private fun SectionHeader(title: String, count: Int, expanded: Boolean, onToggle: () -> Unit) {
  Row(
    Modifier.fillMaxWidth()
      .height(48.dp)
      .clickable(onClick = onToggle),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      CountBadge(count)
    }
    Icon(
      painterResource(Res.drawable.ic_arrow_forward_ios),
      contentDescription = null,
      // The asset points right: up when open, down when folded.
      Modifier.size(20.dp)
        .rotate(if (expanded) -90f else 90f),
      tint = MaterialTheme.colorScheme.primary,
    )
  }
}


/** Mobile, fixed and phone-mic cards share one anatomy; [tag] and [card]'s state set them apart. */
@Composable
private fun SessionCard(
  card: ActiveSessionCard,
  now: Instant,
  tag: String,
  noDataBody: String,
  onSelectStream: (sensorName: String) -> Unit,
  onFinish: () -> Unit,
  onSeeDetails: () -> Unit,
  onReconnect: () -> Unit = {},
) {
  val strings = LocalStrings.current
  Column(
    Modifier.fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    CardHeader(card, now, tag)
    when (card.state) {
      CardState.Live -> StreamPicker(card, onSelectStream)
      CardState.NoDataYet -> {
        StreamPicker(card, onSelectStream)
        Waiting(strings.activeNoDataTitle, noDataBody, spinner = 72.dp)
      }

      CardState.ConnectionLost -> Waiting(
        strings.activeLookingTitle,
        strings.activeLookingBody,
        spinner = 32.dp
      )

      // Figma 1107:16828: the wait box gains a Reconnect button.
      CardState.Offline -> Waiting(strings.activeReconnectingTitle, strings.activeReconnectingBody, spinner = 32.dp) {
        val colors = MaterialTheme.colorScheme
        CardButton(strings.activeReconnect, onReconnect, colors.primary, colors.onSurface, Modifier.fillMaxWidth())
      }
    }
    Footer(card.state, onFinish, onSeeDetails)
  }
}

@Composable
private fun CardHeader(card: ActiveSessionCard, now: Instant, tag: String) {
  val strings = LocalStrings.current
  Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      TintedTag(tag)
      when (card.state) {
        CardState.Live -> card.selected?.level?.let { LevelBadge(it) }
        CardState.NoDataYet -> Unit // Figma 1009:28154: nothing to grade yet
        CardState.ConnectionLost -> AlertBadge(Res.drawable.ic_bluetooth_disabled, strings.activeConnectionLost)
        CardState.Offline -> AlertBadge(Res.drawable.ic_wifi_alert, strings.activeOffline)
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          card.name,
          style = MaterialTheme.typography.titleSmallEmphasized,
          color = MaterialTheme.colorScheme.onSurface
        )
        // Same start - newest measurement line as the details screen, its own row (2026-11-24).
        card.startTime?.let { start ->
          Text(
            LocalDateFormatter.current.range(start, card.lastSeen, card.timeZone),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        DotSeparatedRow(
          card.meta(now, strings),
          MaterialTheme.typography.bodySmall,
          MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      if (card.tags.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          card.tags.forEach { UserTag(it) }
        }
      }
    }
  }
}

/** Same pill shape as [LevelBadge], in the "unhealthy" red: "Connection lost" (mobile), "Offline" (fixed). */
@Composable
private fun AlertBadge(icon: DrawableResource, text: String) {
  val badge = LocalAqBadgeColors.current
  val red = badge.content.unhealthy
  Row(
    Modifier.clip(RoundedCornerShape(100.dp))
      .background(badge.container.unhealthy)
      .padding(horizontal = 8.dp, vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painterResource(icon),
      contentDescription = null,
      Modifier.size(12.dp),
      tint = red
    )
    Text(
      text,
      style = MaterialTheme.typography.labelSmall,
      color = red
    )
  }
}

/** Stream chips, then the selected stream's newest value with its level dot. */
@Composable
private fun StreamPicker(card: ActiveSessionCard, onSelectStream: (String) -> Unit) {
  val strings = LocalStrings.current
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Row(
      Modifier.horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
          ?: Box(
            Modifier.size(10.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.outlineVariant)
          )
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
private fun Waiting(title: String, body: String, spinner: Dp, action: @Composable () -> Unit = {}) {
  Column(
    Modifier.fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.surface)
      .padding(horizontal = 10.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    LoadingSpinner(size = spinner)
    Column(
      Modifier.padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
      )
      Text(
        body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
    action()
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
private fun Footer(state: CardState, onFinish: () -> Unit, onSeeDetails: () -> Unit) {
  val strings = LocalStrings.current
  val colors = MaterialTheme.colorScheme
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    HorizontalDivider(color = colors.surfaceContainerHighest)
    if (state == CardState.Offline) {
      CardButton(
        strings.activeFinishAnyway,
        onFinish,
        colors.error,
        colors.error,
        Modifier.fillMaxWidth(),
        Res.drawable.ic_stop_circle
      )
    } else if (state == CardState.ConnectionLost) {
      // No SD sync yet: the button is there, and does nothing (decided 2026-11-08).
      CardButton(
        strings.activeFinishAndSync,
        onClick = {},
        colors.error,
        colors.error,
        Modifier.fillMaxWidth(),
        Res.drawable.ic_stop_circle
      )
      TextButton(onClick = onFinish) {
        Text(
          strings.activeFinishAndDiscard,
          style = MaterialTheme.typography.labelLarge,
          color = colors.onSurfaceVariant
        )
      }
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CardButton(
          strings.activeFinishRecording,
          onFinish,
          colors.error,
          colors.error,
          Modifier.weight(1f),
          Res.drawable.ic_stop_circle
        )
        CardButton(
          strings.activeSeeDetails,
          onClick = onSeeDetails,
          colors.primary,
          colors.onSurface,
          Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun FinishDialog(sessionName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  val strings = LocalStrings.current
  ConfirmDialog(
    title = strings.activeFinishTitle(sessionName),
    body = strings.activeFinishBody,
    confirmLabel = strings.activeFinishSession,
    dismissLabel = strings.activeKeepRecording,
    onConfirm = onConfirm,
    onDismiss = onDismiss,
  )
}
