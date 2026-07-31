package pl.llp.aircasting.bluetooth.transport

import pl.llp.aircasting.bluetooth.DeviceId
import pl.llp.aircasting.bluetooth.DiscoveredAirBeam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold

fun Flow<DiscoveredAirBeam>.accumulateDistinct(): Flow<List<DiscoveredAirBeam>> =
  runningFold(emptyMap<DeviceId, DiscoveredAirBeam>()) { acc, d -> acc + (d.id to d) }
    .map { it.values.toList() }