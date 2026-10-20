package pl.llp.aircasting.bluetooth.transport

import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.ConnectResult
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import pl.llp.aircasting.bluetooth.FailureReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CompositeAirBeamConnector(
  private val delegates: List<AirBeamConnector>,
) : AirBeamConnector {
  override val supportedTransports = delegates
    .flatMap { it.supportedTransports }
    .toSet()

  override fun scan(): Flow<List<DiscoveredAirBeam>> =
    combine(delegates.map { it.scan() }) { lists ->
      lists.toList()
        .flatten()
    }

  /** No delegate for the transport — e.g. an AirBeam2 on iOS — is a failure, not a crash. */
  override suspend fun connect(target: DiscoveredAirBeam): ConnectResult =
    delegates.firstOrNull { target.device.transport in it.supportedTransports }
      ?.connect(target)
      ?: ConnectResult.Failed(FailureReason.WrongCommunicationSurface)
}
