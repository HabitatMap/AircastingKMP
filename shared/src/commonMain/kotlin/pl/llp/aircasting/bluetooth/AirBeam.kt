package pl.llp.aircasting.bluetooth

import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class Transport { BLE, CLASSIC_SERIAL }

sealed interface AirBeamDevice {
  val transport: Transport
  val reportsOwnState: Boolean

  data object AirBeam2 : AirBeamDevice {
    override val transport = Transport.CLASSIC_SERIAL
    override val reportsOwnState = false
  }

  data object AirBeam3 : AirBeamDevice {
    override val transport = Transport.BLE
    override val reportsOwnState = false
  }

  sealed interface Mini : AirBeamDevice {
    override val transport: Transport get() = Transport.BLE

    data object V1 : Mini {
      override val reportsOwnState = false
    }
    data object V2 : Mini {
      override val reportsOwnState = true
    }
  }
}

data class DiscoveredAirBeam(
  val id: DeviceId,
  val name: String,
  val device: AirBeamDevice,
)

// Android gives a MAC address (String), iOS CoreBluetooth gives a per-app NSUUID
@JvmInline
value class DeviceId(val value: String)