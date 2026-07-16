package com.lunarlogic.aircasting.bluetooth.v2_firmware_specific

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceReportedStateTest {

  // §3: 0x00 Idle -> [0x00, battery_u8]
  @Test
  fun idle_opcode_parses_to_Idle() {
    val status = byteArrayOf(0x00, 0x36) // battery 54%
    assertEquals(DeviceReportedState.Idle, DeviceReportedState.from(status))
  }

  // §3: 0x01 HasSavedSession -> [0x01, battery, uuid16, has_meas, filesize8]
  @Test
  fun has_saved_session_opcode_parses_to_HasSavedSession() {
    val status = byteArrayOf(0x01, 0x36) + ByteArray(25) // rest is payload we ignore
    assertEquals(DeviceReportedState.HasSavedSession, DeviceReportedState.from(status))
  }

  // §3: 0x02 Running -> [0x02, battery, uuid16]
  @Test
  fun running_opcode_parses_to_Running() {
    val status = byteArrayOf(0x02, 0x36) + ByteArray(16)
    assertEquals(DeviceReportedState.Running, DeviceReportedState.from(status))
  }

  @Test
  fun empty_status_parses_to_null() {
    assertNull(DeviceReportedState.from(byteArrayOf()))
  }

  // 0x03 ReadyToSync is a real opcode but a sync-flow status, NOT a connection state -> null
  @Test
  fun unknown_or_non_state_opcode_parses_to_null() {
    assertNull(DeviceReportedState.from(byteArrayOf(0x03, 0x00)))
    assertNull(DeviceReportedState.from(byteArrayOf(0xFF.toByte())))
  }
}