package com.lunarlogic.aircasting.bluetooth

import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class Transport { BLE, CLASSIC_SERIAL }

sealed interface AirBeamDevice {
  val transport: Transport
  val reportsOwnState: Boolean
  val requiresHandshake: Boolean

  data object AirBeam2 : AirBeamDevice {
    override val transport = Transport.CLASSIC_SERIAL
    override val reportsOwnState = false
    override val requiresHandshake = true
  }

  data object AirBeam3 : AirBeamDevice {
    override val transport = Transport.BLE
    override val reportsOwnState = false
    override val requiresHandshake = true
  }

  sealed interface Mini : AirBeamDevice {
    override val transport: Transport get() = Transport.BLE

    data object V1 : Mini {
      override val reportsOwnState = false
      override val requiresHandshake = true
    }
    data object V2 : Mini {
      override val reportsOwnState = true
      override val requiresHandshake = false
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