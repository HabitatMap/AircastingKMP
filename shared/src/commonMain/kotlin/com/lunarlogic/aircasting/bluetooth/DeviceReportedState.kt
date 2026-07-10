package com.lunarlogic.aircasting.bluetooth

sealed interface DeviceReportedState {
  data object Idle : DeviceReportedState
  data object HasSavedSession : DeviceReportedState
  data object Running : DeviceReportedState
}