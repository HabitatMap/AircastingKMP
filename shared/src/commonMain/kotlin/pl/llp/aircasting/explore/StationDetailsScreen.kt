package pl.llp.aircasting.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import org.koin.compose.koinInject
import pl.llp.aircasting.domain.DataSource
import pl.llp.aircasting.domain.MeasurementLevel
import pl.llp.aircasting.domain.SensorThreshold
import pl.llp.aircasting.domain.StationDetails
import pl.llp.aircasting.domain.StationStream
import pl.llp.aircasting.domain.session.SessionType
import pl.llp.aircasting.i18n.LocalStrings
import pl.llp.aircasting.i18n.Strings
import pl.llp.aircasting.map.FullScreenButton
import pl.llp.aircasting.settings.SettingsScaffold
import pl.llp.aircasting.ui.ageLabel
import pl.llp.aircasting.ui.components.DotSeparatedRow
import pl.llp.aircasting.ui.components.ErrorState
import pl.llp.aircasting.ui.components.LevelBadge
import pl.llp.aircasting.ui.components.LevelDot
import pl.llp.aircasting.ui.components.LoadingSpinner
import pl.llp.aircasting.ui.components.StreamChip
import pl.llp.aircasting.ui.components.TintedTag
import pl.llp.aircasting.ui.deviceDisplayName
import pl.llp.aircasting.ui.streamChipLabel
import kotlin.time.Clock

/** Figma "Single station view" (693:1565), header + stats; graph/map/slider come later. */
@Composable
fun StationDetailsScreen(
  state: StationDetailsState,
  onBack: () -> Unit,
  onRetry: () -> Unit,
  onSelectStream: (streamId: Long) -> Unit,
  onThresholdChange: (SensorThreshold) -> Unit,
  graph: @Composable (details: StationDetails, stream: StationStream, threshold: SensorThreshold?, modifier: Modifier) -> Unit,
  /** [fullScreen]: the session map pans and zooms only there; the inline one is static. */
  map: @Composable (details: StationDetails, stream: StationStream, threshold: SensorThreshold?, fullScreen: Boolean, modifier: Modifier) -> Unit,
) {
  val strings = LocalStrings.current
  var showMap by rememberSaveable { mutableStateOf(false) }
  var fullScreen by rememberSaveable { mutableStateOf(false) }
  val content = state as? StationDetailsState.Content
  val stream = content?.selected
  if (fullScreen && content != null && stream != null) {
    FullScreenMap(content, stream, map, onExit = { fullScreen = false }, onThresholdChange)
    return
  }
  // App-bar actions (export, share, favourite, alert) stay hidden until they work.
  SettingsScaffold(title = "", onBack = onBack) { padding ->
    Box(
      Modifier.fillMaxSize()
        .padding(padding)
    ) {
      when (state) {
        StationDetailsState.Loading -> LoadingSpinner(Modifier.align(Alignment.Center))
        StationDetailsState.Error -> ErrorState(onRetry, Modifier.align(Alignment.Center))
        // Fits the screen: the graph/map takes what's left. Below MinContentHeight (small phone,
        // landscape) the column stops shrinking and scrolls instead, so the graph stays usable.
        // A fixed height is what lets weight() work inside verticalScroll.
        is StationDetailsState.Content -> BoxWithConstraints {
          Column(
            Modifier.verticalScroll(rememberScrollState())
              .height(max(maxHeight, MinContentHeight))
              .padding(horizontal = ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            val details = state.details
            val selected = state.selected
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              TintedTag(details.tag(strings))
              // NOW value's level; hidden when the sensor has no local threshold.
              selected?.let { state.levelOf(it.value) }
                ?.let { LevelBadge(it) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                details.title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              // Design's "Location name" line needs reverse geocoding — later slice.
              val clock = koinInject<Clock>()
              // One reading per loaded state, like NearbyStations: the label doesn't tick while open.
              val now = remember(state) { clock.now() }
              DotSeparatedRow(
                listOfNotNull(
                  if (details.isIndoor) strings.placementIndoor else strings.stationOutdoor,
                  details.deviceModel?.let(::deviceDisplayName) ?: details.provider,
                  selected?.measuredAt?.let { ageLabel(it, now, strings) },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
              )
            }
            Row(
              Modifier.horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              details.streams.forEach { stream ->
                StreamChip(
                  streamChipLabel(stream.sensorName),
                  selected = stream.id == state.selectedStreamId,
                  onClick = { onSelectStream(stream.id) },
                )
              }
            }
            selected?.let { stream ->
              Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(
                  strings.stationNow,
                  stream.value,
                  stream.unitSymbol,
                  state.levelOf(stream.value)
                )
                Stat(
                  strings.stationMin24h,
                  stream.min,
                  stream.unitSymbol,
                  state.levelOf(stream.min)
                )
                Stat(
                  strings.stationMax24h,
                  stream.max,
                  stream.unitSymbol,
                  state.levelOf(stream.max)
                )
              }
              // Picker hugs the graph/map it switches: 8 dp, not the column's 16.
              Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (details.hasMap) {
                  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(false, true).forEachIndexed { index, isMap ->

                      SegmentedButton(
                        selected = showMap == isMap,
                        onClick = { showMap = isMap },
                        shape = SegmentedButtonDefaults.itemShape(index, count = 2),
                      ) { Text(if (isMap) strings.detailsMap else strings.detailsGraph) }
                    }
                  }
                }
                if (showMap && details.hasMap) {
                  // Figma 599:12567: edge to edge (412 × 408 there; here it takes the free height).
                  Box(
                    Modifier.weight(1f)
                      .bleed(ScreenPadding)
                  ) {
                    map(details, stream, state.threshold, false, Modifier.fillMaxSize())
                    FullScreenButton(
                      false,
                      onClick = { fullScreen = true },
                      Modifier.align(Alignment.TopEnd)
                        .padding(4.dp)
                    )
                  }
                } else {
                  // Figma draws the graph edge to edge, through the screen padding.
                  graph(
                    details,
                    stream,
                    state.threshold,
                    Modifier.weight(1f)
                      .bleed(ScreenPadding)
                  )
                }
              }
              ThresholdControls(state, stream, onThresholdChange)
            }
          }
        }
      }
    }
  }
}

private val ScreenPadding = 24.dp

/** Below this the details column scrolls rather than squeezing the graph/map further. */
private val MinContentHeight = 640.dp

/** Widens the child by [padding] on each side, undoing the parent's horizontal padding. */
private fun Modifier.bleed(padding: Dp): Modifier = layout { measurable, constraints ->
  val extra = padding.roundToPx() * 2
  val width = constraints.maxWidth + extra
  val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
  layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}

private fun StationDetails.tag(strings: Strings): String = when (source) {
  DataSource.Government -> strings.govMonitor
  DataSource.PhoneMic -> strings.activeTagPhoneMic
  DataSource.AirBeam -> when (type) {
    SessionType.FIXED -> strings.exploreSourceAirBeam.uppercase() // "AIRBEAM FIXED"
    SessionType.MOBILE -> strings.activeTagAirBeamMobile
  }
}

@Composable
/** [value] null (no reading) → a dash. */
private fun Stat(label: String, value: Int?, unit: String, level: MeasurementLevel?) {
  Column {
    Text(
      label,
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      level?.let { LevelDot(it) }
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          value?.toString() ?: "–",
          Modifier.alignByBaseline(),
          style = MaterialTheme.typography.headlineSmallEmphasized,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          unit,
          Modifier.alignByBaseline(),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}

/** Outdoor fixed stations need a position; mobile sessions draw their own track. */
private val StationDetails.hasMap: Boolean
  get() = !isIndoor && (type == SessionType.MOBILE || location != null)

/** Slider + Thresholds sheet. Shared by the details column and the full-screen map. */
@Composable
private fun ThresholdControls(
  state: StationDetailsState.Content, stream: StationStream, onChange: (SensorThreshold) -> Unit,
  modifier: Modifier = Modifier,
) {
  // No threshold for this sensor → nothing to edit (and the chart/map are uncoloured).
  val threshold = state.threshold ?: return
  // Screen-local UI state; saveable, so rotation keeps the sheet open.
  var editing by rememberSaveable { mutableStateOf(false) }
  ThresholdSlider(
    threshold,
    stream.unitSymbol,
    onChange = onChange,
    onEdit = { editing = true },
    modifier = modifier
  )
  if (editing) ThresholdsSheet(
    threshold,
    default = state.defaultThreshold,
    onDismiss = { editing = false },
    onSave = onChange
  )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FullScreenMap(
  state: StationDetailsState.Content,
  stream: StationStream,
  map: @Composable (StationDetails, StationStream, SensorThreshold?, Boolean, Modifier) -> Unit,
  onExit: () -> Unit,
  onThresholdChange: (SensorThreshold) -> Unit,
) {
  BackHandler(onBack = onExit)
  Column(
    Modifier.fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .systemBarsPadding()
  ) {
    Box(
      Modifier.weight(1f)
        .fillMaxWidth()
    ) {
      map(state.details, stream, state.threshold, true, Modifier.fillMaxSize())
      FullScreenButton(
        true,
        onClick = onExit,
        Modifier.align(Alignment.TopEnd)
          .padding(4.dp)
      )
    }
    ThresholdControls(
      state,
      stream,
      onThresholdChange,
      Modifier.padding(horizontal = ScreenPadding, vertical = 16.dp)
    )
  }
}