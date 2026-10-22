package pl.llp.aircasting.bluetooth

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** The link as the recording side sees it: tests set its answers and push its state. */
class FakeAirBeamLink(var configureResult: ConfigResult = ConfigResult.Success) : AirBeamLink {
  override val state = MutableStateFlow<LinkState>(LinkState.Idle)

  private val sent = Channel<SensorReading>(Channel.UNLIMITED)
  override val readings: Flow<SensorReading> = sent.receiveAsFlow()

  val configured = mutableListOf<SessionConfig>()
  var disconnects = 0
    private set

  fun send(reading: SensorReading) {
    sent.trySend(reading)
  }

  val connects = mutableListOf<DiscoveredAirBeam>()
  /** Makes every [connect] settle as [LinkState.Failed] with this reason. */
  var connectFailure: FailureReason? = null

  override suspend fun connect(target: DiscoveredAirBeam): LinkState {
    connects += target
    val settled = connectFailure?.let { LinkState.Failed(it) } ?: LinkState.Connected(target)
    return settled.also { state.value = it }
  }

  override suspend fun configure(config: SessionConfig): ConfigResult {
    configured += config
    return configureResult
  }

  override suspend fun disconnect() {
    disconnects++
    state.value = LinkState.Idle
  }
}