package com.lunarlogic.aircasting.bluetooth.v2_firmware_specific

sealed interface DeviceReportedState {
  data object Idle : DeviceReportedState
  data object HasSavedSession : DeviceReportedState
  data object Running : DeviceReportedState
}