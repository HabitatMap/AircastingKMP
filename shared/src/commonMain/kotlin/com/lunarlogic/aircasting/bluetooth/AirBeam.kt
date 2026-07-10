package com.lunarlogic.aircasting.bluetooth

import kotlin.jvm.JvmInline

sealed interface AirBeamDevice {
  val reportsOwnState: Boolean
  data object AirBeam3 : AirBeamDevice { override val reportsOwnState = false }

  sealed interface Mini : AirBeamDevice {
    data object V1 : Mini { /* legacy service, handshake yes, state no */
      override val reportsOwnState: Boolean
        get() = false
    }
    data object V2 : Mini { /* V2 service, handshake no, reports state */
      override val reportsOwnState: Boolean
        get() = true
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