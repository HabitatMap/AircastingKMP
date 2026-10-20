package pl.llp.aircasting.bluetooth.transport

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import pl.llp.aircasting.bluetooth.AirBeamConnection
import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.AirBeamDevice
import pl.llp.aircasting.bluetooth.ConfigResult
import pl.llp.aircasting.bluetooth.ConnectResult
import pl.llp.aircasting.bluetooth.ConnectionStatus
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.SensorReading
import pl.llp.aircasting.bluetooth.SessionConfig
import pl.llp.aircasting.bluetooth.Transport

/**
 * `connect` answers with whatever the test queued via [willConnect], in order. With nothing
 * queued it suspends until something is — that is how a test holds a connect attempt open.
 */
class FakeConnector(
  override val supportedTransports: Set<Transport>,
  private val scanFlow: Flow<List<DiscoveredAirBeam>> = flowOf(emptyList()),
) : AirBeamConnector {
  val targets = mutableListOf<DiscoveredAirBeam>()
  val connectedTarget get() = targets.lastOrNull()
  private val results = Channel<ConnectResult>(Channel.UNLIMITED)

  fun willConnect(vararg next: ConnectResult) = next.forEach { results.trySend(it) }

  override fun scan() = scanFlow

  override suspend fun connect(target: DiscoveredAirBeam): ConnectResult {
    targets += target
    return results.receive()
  }
}

class FakeConnection(
  override val device: AirBeamDevice = AirBeamDevice.AirBeam3,
  var configureResult: ConfigResult = ConfigResult.Success,
  var resumeResult: ConfigResult = ConfigResult.Success,
) : AirBeamConnection {
  override val status = MutableStateFlow(ConnectionStatus.Ready)
  override val deviceState = null
  private val sent = Channel<SensorReading>(Channel.UNLIMITED)
  override val readings = sent.receiveAsFlow()

  val configured = mutableListOf<SessionConfig>()
  val resumed = mutableListOf<SessionConfig.Mobile>()
  var disconnects = 0
    private set

  fun drop() {
    status.value = ConnectionStatus.DisconnectedUnexpectedly
  }

  fun send(reading: SensorReading) {
    sent.trySend(reading)
  }

  override suspend fun configure(config: SessionConfig): ConfigResult {
    configured += config
    return configureResult
  }

  override suspend fun resumeMobileSession(config: SessionConfig.Mobile): ConfigResult {
    resumed += config
    return resumeResult
  }

  override suspend fun disconnect() {
    disconnects++
    status.compareAndSet(ConnectionStatus.Ready, ConnectionStatus.Disconnected)
    sent.close()
  }
}

fun FakeConnection.connected() = ConnectResult.Connected(this)
