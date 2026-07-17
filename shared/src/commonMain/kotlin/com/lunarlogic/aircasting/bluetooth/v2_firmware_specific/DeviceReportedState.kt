package com.lunarlogic.aircasting.bluetooth.v2_firmware_specific

sealed interface DeviceReportedState {
  data object Idle : DeviceReportedState
  data object HasSavedSession : DeviceReportedState
  data object Running : DeviceReportedState

  companion object {
    fun from(status: ByteArray): DeviceReportedState? = when (status.firstOrNull()) {
      0x00.toByte() -> Idle
      0x01.toByte() -> HasSavedSession
      0x02.toByte() -> Running
      else -> null // empty, ReadyToSync(0x03), or unknown
    }
  }
}