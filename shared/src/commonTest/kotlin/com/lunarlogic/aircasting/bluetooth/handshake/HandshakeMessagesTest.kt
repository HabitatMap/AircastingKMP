package com.lunarlogic.aircasting.bluetooth.handshake

import kotlin.test.Test
import kotlin.test.assertContentEquals

class HandshakeMessagesTest {

  @Test
  fun uuid_message_wraps_raw_ascii_payload_under_0x04() {
    // 0xFE, 0x04, "test-uuid" as raw ASCII, 0xFF — NOT hex-encoded, NOT base64
    val expected = byteArrayOf(
      0xFE.toByte(), 0x04,
      0x74, 0x65, 0x73, 0x74, 0x2d, 0x75, 0x75, 0x69, 0x64, // t e s t - u u i d
      0xFF.toByte(),
    )
    assertContentEquals(expected, HandshakeMessages.uuidMessage("test-uuid"))
  }

  @Test
  fun auth_token_message_frames_base64_of_token_colon_X_under_0x05() {
    // "abc" -> raw "abc:X" -> Base64 (no wrap) "YWJjOlg=" -> framed
    val expected = byteArrayOf(0xFE.toByte(), 0x05) +
      "YWJjOlg=".encodeToByteArray() +
      byteArrayOf(0xFF.toByte())
    assertContentEquals(expected, HandshakeMessages.authTokenMessage("abc"))
  }
}