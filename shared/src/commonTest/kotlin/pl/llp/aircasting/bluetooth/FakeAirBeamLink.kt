package pl.llp.aircasting.bluetooth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** The link as the recording side sees it: tests set its answers and push its state. */
class FakeAirBeamLink(var configureResult: ConfigResult = ConfigResult.Success) : AirBeamLink {
  override val state = MutableStateFlow<LinkState>(LinkState.Idle)

  private val sent = Channel<SensorReading>(Channel.UNLIMITED)
  override val readings: Flow<SensorReading> = sent.receiveAsFlow()

  /** When set, [connect] waits in [LinkState.Connecting] until it completes: an attempt still in flight. */
  var pendingConnect: CompletableDeferred<Unit>? = null

  override suspend fun connect(target: DiscoveredAirBeam): LinkState {
    connects += target
    pendingConnect?.let {
      state.value = LinkState.Connecting(target)
      it.await()
    }
    val settled = connectFailure?.let { LinkState.Failed(it) } ?: LinkState.Connected(target)
    return settled.also { state.value = it }
  }

  val configured = mutableListOf<SessionConfig>()
  var disconnects = 0
    private set

  fun send(reading: SensorReading) {
    sent.trySend(reading)
  }

  val connects = mutableListOf<DiscoveredAirBeam>()
  /** Makes every [connect] settle as [LinkState.Failed] with this reason. */
  var connectFailure: FailureReason? = null

  override suspend fun configure(config: SessionConfig): ConfigResult {
    configured += config
    return configureResult
  }

  /** The link state at each [endSession]: shows whether it came before the disconnect. */
  val sessionsEndedWhile = mutableListOf<LinkState>()

  override suspend fun endSession() {
    sessionsEndedWhile += state.value
  }

  override suspend fun disconnect() {
    disconnects++
    state.value = LinkState.Idle
  }
}