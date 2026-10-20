package pl.llp.aircasting.bluetooth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** The app's view of its AirBeam link. Holds no platform handles, so it is safe to hand to UI. */
sealed interface LinkState {
  data object Idle : LinkState
  data class Connecting(val target: DiscoveredAirBeam) : LinkState
  data class Connected(val target: DiscoveredAirBeam) : LinkState
  /** A mobile session's link dropped; [attempt] counts from 1 and has no cap. */
  data class Reconnecting(val target: DiscoveredAirBeam, val attempt: Int) : LinkState
  data class Failed(val reason: FailureReason) : LinkState
}

/**
 * The one AirBeam connection of the app, owned for the life of the process — not by a screen.
 * Connection-level only: it knows whether a mobile session was started over the link (that is
 * what it reconnects for), but sessions, their status and their data belong to the recording side.
 */
interface AirBeamLink {
  val state: StateFlow<LinkState>

  /** Readings of whichever connection is current; carries on across reconnects. One collector. */
  val readings: Flow<SensorReading>

  /** Suspends until the attempt settles: [LinkState.Connected] or [LinkState.Failed]. */
  suspend fun connect(target: DiscoveredAirBeam): LinkState

  /** A successful [SessionConfig.Mobile] arms reconnecting; anything else disarms it. */
  suspend fun configure(config: SessionConfig): ConfigResult

  /** Ends the link and any reconnecting. Stop and standalone mode both land here. */
  suspend fun disconnect()
}

class DefaultAirBeamLink(
  private val connector: AirBeamConnector,
  /** Process-lifetime; the link's work runs here so it outlives the screens that start it. */
  private val scope: CoroutineScope,
  /** Legacy `AirBeamReconnector.RECONNECTION_TRIES_INTERVAL`. */
  private val reconnectInterval: Duration = 2.seconds,
) : AirBeamLink {
  private val log = Logger.withTag("AirBeamLink")

  private val _state = MutableStateFlow<LinkState>(LinkState.Idle)
  override val state: StateFlow<LinkState> = _state.asStateFlow()

  /**
   * The newest connection. Kept after it drops until a new one replaces it, so its buffered
   * readings still drain; only `state` says whether it is usable.
   */
  private val current = MutableStateFlow<AirBeamConnection?>(null)

  /** What a dropped link reconnects for. Set by a successful mobile [configure]. */
  private val mobileSession = MutableStateFlow<SessionConfig.Mobile?>(null)

  @OptIn(ExperimentalCoroutinesApi::class)
  override val readings: Flow<SensorReading> = current.flatMapLatest { it?.readings ?: emptyFlow() }

  /** Serializes [connect] / [disconnect]: each one cancels and awaits the previous [link]. */
  private val commands = Mutex()
  private var link: Job? = null

  override suspend fun connect(target: DiscoveredAirBeam): LinkState {
    val settled = CompletableDeferred<LinkState>()
    commands.withLock {
      stop()
      link = scope.launch { run(target, settled) }
    }
    return settled.await()
  }

  override suspend fun configure(config: SessionConfig): ConfigResult {
    val connection = current.value?.takeIf { _state.value is LinkState.Connected }
      ?: return ConfigResult.NotConnected
    val result = connection.configure(config)
    if (result == ConfigResult.Success) mobileSession.value = config as? SessionConfig.Mobile
    return result
  }

  override suspend fun disconnect() = commands.withLock {
    stop()
    _state.value = LinkState.Idle
  }

  private suspend fun stop() {
    link?.cancelAndJoin()
    link = null
    mobileSession.value = null
  }

  /** One link's whole life: connect, then hold it — and win it back — until cancelled. */
  private suspend fun run(target: DiscoveredAirBeam, settled: CompletableDeferred<LinkState>) {
    try {
      _state.value = LinkState.Connecting(target)
      var connection = when (val result = connectOnce(target)) {
        is ConnectResult.Failed -> return settle(settled, LinkState.Failed(result.reason))
        is ConnectResult.Connected -> result.connection
      }
      while (true) {
        current.value = connection
        settle(settled, LinkState.Connected(target))

        connection.status.first { it != ConnectionStatus.Ready }
        connection.disconnect()
        val session = mobileSession.value
          ?: return settle(settled, LinkState.Failed(FailureReason.ConnectionLost))
        log.w { "${target.name} dropped mid-session, reconnecting" }
        connection = reconnect(target, session)
      }
    } finally {
      settled.complete(LinkState.Idle) // cancelled before settling: someone disconnected
      // A connection that is no longer Ready was already released by the loop above.
      current.getAndUpdate { null }
        ?.takeIf { it.status.value == ConnectionStatus.Ready }
        ?.let { withContext(NonCancellable) { it.disconnect() } }
    }
  }

  private fun settle(settled: CompletableDeferred<LinkState>, state: LinkState) {
    _state.value = state
    settled.complete(state)
  }

  /** Legacy `AirBeamReconnector`: every [reconnectInterval], no cap; only [disconnect] stops it. */
  private suspend fun reconnect(target: DiscoveredAirBeam, session: SessionConfig.Mobile): AirBeamConnection {
    var attempt = 1
    while (true) {
      _state.value = LinkState.Reconnecting(target, attempt++)
      delay(reconnectInterval)
      val connection = (connectOnce(target) as? ConnectResult.Connected)?.connection ?: continue
      val resumed = connection.resumeMobileSession(session)
      if (resumed == ConfigResult.Success) return connection
      log.w { "${target.name} reconnected but did not resume: $resumed" }
      connection.disconnect()
    }
  }

  /** The connector reports failures as values; this keeps a stray platform throw from crashing [scope]. */
  private suspend fun connectOnce(target: DiscoveredAirBeam): ConnectResult = try {
    connector.connect(target)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    log.e(e) { "Connecting to ${target.name} threw" }
    ConnectResult.Failed(FailureReason.HandshakeFailed)
  }
}
